typealias MessageAction = (msg: TMessage) -> Unit

data class CommandAction(val command: String, val description: String, val action: MessageAction)

class MessageHandlerBuilder {
    val commands = mutableListOf<CommandAction>()
    var textAction: MessageAction? = null

    fun onCommand(command: String, description: String, action: MessageAction) {
        commands += CommandAction(command, description, action)
    }

    fun onTextMessage(action: MessageAction) {
        textAction = action
    }
}

class MessageHandler(listenerBuilder: MessageHandlerBuilder.() -> Unit) {
    init {
        val builder = MessageHandlerBuilder()
        listenerBuilder(builder)

        val commands = builder.commands
        val textAction = builder.textAction

        Api.setMyCommands(TSetCommandsParams(commands.map { TBotCommand(it.command, it.description) }))

        var nextUpdate: Long? = null
        while (true) {
            Thread.sleep(1000)

            val updates = Api.getUpdates(TUpdatesParams(nextUpdate)) ?: continue
            for (update in updates) {
                val msg = update.message ?: continue
                val entity = msg.entities?.firstOrNull()

                if (entity != null && entity.type == TMessageEntityType.BotCommand) {
                    val text = msg.text ?: continue
                    val commandStr = text.substring(entity.offset, entity.offset + entity.length)
                    val command = commands.find { "/${it.command}" == commandStr } ?: continue
                    command.action(msg)
                } else if (textAction != null) {
                    textAction(msg)
                }
            }

            updates.lastOrNull()?.let { nextUpdate = it.updateId + 1 }
        }
    }

}
