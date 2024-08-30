package org.taktik.icure.asynclogic.bridge.auth

import com.icure.sdk.auth.JwtBearer
import com.icure.sdk.auth.services.ProxyAuthProvider
import com.icure.sdk.utils.InternalIcureApi

@OptIn(InternalIcureApi::class)
class KmehrAuthProvider (
	private val token: String
): ProxyAuthProvider() {
	override suspend fun getToken(): JwtBearer = JwtBearer(token)
}