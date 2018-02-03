package com.mingshz.tools.entrance

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File
import java.io.FileInputStream
import java.io.IOException


/**
 * @author CJ
 */
class Loader {
    @Throws(IOException::class, InterruptedException::class)
    private fun work(workingDir: File, entrance: Entrance) {
        // 形成Entrance的结构
        System.exit(DockerFileBuilder.create()
                .forEntrance(entrance)
                .build(workingDir, workingDir))
    }

    fun main(args: Array<String>) {

        // create Options object
        val options = Options()
        // add t option
        options.addOption("model", true, "1 for build docker image(default), 2 for working with ALI LB.")
        options.addOption("wd", true, "working dir(default CWD).")
        options.addOption("f", true, "json file for build(default entrance.json).")
        options.addOption("v", "display this help.")

        val parser = DefaultParser()
        val cmd = parser.parse(options, args)

        if (cmd.hasOption("v")) {
            // automatically generate the help statement
            val formatter = HelpFormatter()
            formatter.printHelp("java -jar ....jar", options)
            return
        }

        // 直接读取 entrance.json 并且在本地工作
        val fileName = if (cmd.hasOption("f"))
            cmd.getOptionValue("f")
        else
            "./entrance.json"

        val workingPath = if (cmd.hasOption("wd"))
            cmd.getOptionValue("wd")
        else
            "./"

        FileInputStream(fileName).use { fileInputStream ->
            val entrance = Utils.readFromJson(fileInputStream)

            work(File(workingPath), entrance)

        }
    }
}

fun main(args: Array<String>){
    Loader().main(args)
}