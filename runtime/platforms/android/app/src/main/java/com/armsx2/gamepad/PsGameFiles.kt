package com.armsx2.gamepad

import java.io.File

object PsGameFiles {
    data class Image(val file:File,val directory:File)
    private val extensions=setOf("iso","bin","img","chd")
    fun discover(root:File):List<Image> {
        val canonical=root.canonicalFile
        if(!canonical.isDirectory)return emptyList()
        return canonical.walkTopDown().maxDepth(5).onEnter {
            MaterialFiles.inside(it,canonical) && (it==canonical ||
                (!it.name.startsWith('.') && it.name.lowercase() !in setOf("memcards","sstates","textures","covers")))
        }.filter {it.isFile && !it.name.startsWith('.') && it.extension.lowercase() in extensions && MaterialFiles.inside(it,canonical)}
            .map {file ->
                val relative=file.relativeTo(canonical)
                // Flat ROMs receive a stable sibling save folder without moving their image.
                val directory=if(relative.parent==null) File(canonical,file.nameWithoutExtension)
                    else File(canonical,relative.invariantSeparatorsPath.substringBefore('/'))
                check(!directory.exists() || directory.isDirectory){"游戏存档目录冲突：${directory.name}"}
                Image(file,directory)
            }.sortedBy {it.file.absolutePath.lowercase()}.toList()
    }
}
