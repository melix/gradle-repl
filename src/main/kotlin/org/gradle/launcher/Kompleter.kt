package org.gradle.launcher

import jline.console.completer.StringsCompleter

class Kompleter(tasks: Set<String>) : StringsCompleter(tasks) {
    override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>?): Int {
        val success = super.complete(buffer, cursor, candidates)
        if (success == 0) {
            return 0
        }
        if (buffer != null) {
            if (buffer.length == 0 || buffer.get(0) != ':') {
                return super.complete(':' + buffer, cursor, candidates)
            }
        }
        return -1;
    }
}