package uz.vv.mediaservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class MediaServiceApplication

fun main(args: Array<String>) {
    runApplication<MediaServiceApplication>(*args)
}
