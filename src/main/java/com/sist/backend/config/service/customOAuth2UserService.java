package com.sist.backend.config.service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class customOAuth2UserService extends DefaultOAuth2UserService {
	private final RestClient restClient = RestClient.create();
	

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		if (userRequest.getClientRegistration().getRegistrationId().equals("naver")) {
			String userInfoUri = userRequest.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUri();
			String tokenValue = userRequest.getAccessToken()
				.getTokenValue();
			ResponseEntity<NaverUserInfoDto> response = restClient.get() //get 방식으로 설정
				.uri(userInfoUri)
				.headers(httpHeaders -> httpHeaders.setBearerAuth(tokenValue))
				.retrieve()
				.toEntity(NaverUserInfoDto.class);
			if (response.hasBody()) {
				NaverUserInfoDto oauthResponse = response.getBody();
                if (oauthResponse != null && oauthResponse.response() instanceof Map) {
					Map<String, Object> userAttributes = (Map<String, Object>) oauthResponse.response();
					userAttributes.computeIfAbsent(StandardClaimNames.SUB, key -> userAttributes.get("id"));
					userAttributes.remove("id");
					Set authorities = new LinkedHashSet<>();
					authorities.add(new OAuth2UserAuthority(userAttributes));
					OAuth2AccessToken token = userRequest.getAccessToken();
					for (String authority : token.getScopes()) {
						authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
					}
					return new DefaultOAuth2User(authorities, userAttributes, "name");
				}
			}
		}
		return super.loadUser(userRequest);
	}
	private record NaverUserInfoDto(
		String resultcode,
		String message,
		Map<String, Object> response) {
	}
}
