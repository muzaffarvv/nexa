package uz.vv.userservice

import org.springframework.stereotype.Component

interface BaseMapper<E, R> {
    fun toDto(entity: E): R
    fun toDtoList(entities: List<E>): List<R> = entities.map { toDto(it) }
}
@Component
class UserMapper : BaseMapper<User, UserResponseDto> {

    override fun toDto(entity: User): UserResponseDto =
        UserResponseDto(
            id = entity.id!!,
            fullName = entity.fullName,
            username = entity.username,
            phoneNumber = entity.phoneNumber,
            bio = entity.bio,
            mediaKey = entity.mediaKey ?: "",
            age = entity.age,
            isPrivate = entity.isPrivate
        )
}
