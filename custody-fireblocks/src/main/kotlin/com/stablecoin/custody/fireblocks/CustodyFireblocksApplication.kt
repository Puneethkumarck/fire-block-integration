package com.stablecoin.custody.fireblocks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CustodyFireblocksApplication

fun main(args: Array<String>) {
    runApplication<CustodyFireblocksApplication>(*args)
}
