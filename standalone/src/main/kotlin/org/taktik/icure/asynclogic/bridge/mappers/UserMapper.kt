package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.User as SdkUser
import org.springframework.stereotype.Service
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2Mapper

@Service
class UserMapper(
	objectMapper: ObjectMapper,
	userMapper: UnsecureUserV2Mapper
) : AbstractEntityMapper<User, SdkUser, UserDto>(
	objectMapper,
	SdkUser::class,
	UserDto::class.java,
	userMapper::map,
	{userMapper.map(it.copy(
		authenticationTokens = it.authenticationTokens.mapValues { (_, token) -> token.copy(token = "*") }
	)) }
)