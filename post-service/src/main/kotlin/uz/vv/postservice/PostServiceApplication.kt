package uz.vv.postservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(
    repositoryBaseClass = BaseRepoImpl::class
)
class PostServiceApplication

fun main(args: Array<String>) {
    runApplication<PostServiceApplication>(*args)
}
