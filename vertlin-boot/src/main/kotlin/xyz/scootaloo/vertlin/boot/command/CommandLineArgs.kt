package xyz.scootaloo.vertlin.boot.command

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午5:08
 */
class CommandLineArgs private constructor() {

    private val options = HashMap<String, String>()
    private val nonOptions = HashSet<String>()

    fun containsOption(optionName: String): Boolean {
        return options.containsKey(optionName)
    }

    fun containsNonOption(optionName: String): Boolean {
        return nonOptions.contains(optionName)
    }

    fun getOptionValue(optionName: String): String {
        return options[optionName]!!
    }

    fun toMap(): Map<String, String> {
        return options.toMap()
    }

    private fun addOption(optionName: String, optionValue: String) {
        options[optionName] = optionValue
    }

    private fun addNonOption(optionName: String) {
        nonOptions.add(optionName)
    }

    companion object {

        fun parse(args: Array<String>): CommandLineArgs {
            val store = CommandLineArgs()
            for (arg in args) {
                if (arg.startsWith("--") || arg.startsWith("-")) {
                    val optionText = arg.trimStart('-')
                    var optionName = ""
                    var optionValue = ""

                    val eqIdx = optionText.indexOf('=')
                    if (eqIdx >= 0) { // 参数项包含一个=符号
                        optionName = optionText.substring(0, eqIdx)
                        optionValue = optionText.substring(eqIdx + 1)
                    } else {
                        optionName = optionText
                    }

                    if (optionName.isEmpty() || optionValue.isEmpty()) {
                        throw IllegalAccessException("Invalid argument syntax: '$arg'")
                    }

                    store.addOption(optionName, optionValue)
                } else {
                    store.addNonOption(arg)
                }
            }
            return store
        }

    }

}
