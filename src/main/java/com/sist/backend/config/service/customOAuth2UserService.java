package com.sist.backend.config.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
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

import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class customOAuth2UserService extends DefaultOAuth2UserService {
	private final RestClient restClient = RestClient.create();
	private final CustomerService customerService;
	

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		log.info("=== OAuth2 사용자 정보 로드 시작 ===");
		log.info("userRequest: {}", userRequest.getClientRegistration());
		String provider = userRequest.getClientRegistration().getRegistrationId();
		Integer providerCode=null;
		if (provider.equals("naver")) {
			providerCode = 0;
		} else if (provider.equals("kakao")) {
			providerCode = 1;
		} else if (provider.equals("google")) {
			providerCode = 2;
		}

		if (provider.equals("naver")) {
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
					userAttributes.computeIfAbsent(StandardClaimNames.PHONE_NUMBER, key -> userAttributes.get("mobile"));
					userAttributes.remove("mobile");
					log.info("userAttributes: {}", userAttributes);

					newCustomer(userAttributes, providerCode);
					Set authorities = new LinkedHashSet<>();
					authorities.add(new OAuth2UserAuthority(userAttributes));
					
					// OAuth2 Scope 기반 권한 추가
					OAuth2AccessToken token = userRequest.getAccessToken();
					for (String authority : token.getScopes()) {
						authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
					}
					
					// 사용자 역할(ROLE) 권한 추가 - SecurityJavaConfig에서 사용
					// OAuth2 로그인 사용자는 기본적으로 CUSTOMER 역할을 가짐
					authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
					
					log.info("설정된 Authorities: {}", authorities);
					return new DefaultOAuth2User(authorities, userAttributes, "name");
				}
			}
			//네이버일 때 끝
		}else if (provider.equals("kakao")) {
			String userInfoUri = userRequest.getClientRegistration()
				.getProviderDetails()
				.getUserInfoEndpoint()
				.getUri();
			String tokenValue = userRequest.getAccessToken()
				.getTokenValue();
			ResponseEntity<KakaoUserInfoDto> response = restClient.get() //get 방식으로 설정
				.uri(userInfoUri)
				.headers(httpHeaders -> httpHeaders.setBearerAuth(tokenValue))
				.retrieve()
				.toEntity(KakaoUserInfoDto.class);
			if (response.hasBody()) {
				KakaoUserInfoDto oauthResponse = response.getBody();
				Map<String, Object> userAttributes = oauthResponse.kakao_account();
				userAttributes.put("nickname", oauthResponse.properties().get("nickname"));
				userAttributes.put("id", oauthResponse.id().toString());
				
				// 카카오 birthday 변환 (MMdd 형식 → MM-dd 형식)
				Object birthdayObj = userAttributes.get("birthday");
				if (birthdayObj != null) {
					String birthdayStr = birthdayObj.toString();
					// "1121" 형식이면 "11-21"로 변환
					if (birthdayStr.length() == 4 && !birthdayStr.contains("-")) {
						String month = birthdayStr.substring(0, 2);
						String day = birthdayStr.substring(2, 4);
						String formattedBirthday = month + "-" + day;
						userAttributes.put("birthday", formattedBirthday);
						log.info("카카오 birthday 변환: {} → {}", birthdayStr, formattedBirthday);
					}
				}
				
				log.info("userAttributes: {}", userAttributes);
				
				newCustomer(userAttributes, providerCode);

				Set authorities = new LinkedHashSet<>();
				authorities.add(new OAuth2UserAuthority(userAttributes));
				
				// OAuth2 Scope 기반 권한 추가
				OAuth2AccessToken token = userRequest.getAccessToken();
				for (String authority : token.getScopes()) {
					authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
				}
				authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
				log.info("설정된 Authorities: {}", authorities);
				return new DefaultOAuth2User(authorities, userAttributes, "name");
			}
		}
		return super.loadUser(userRequest);
	}
	private record NaverUserInfoDto(
		String resultcode,
		String message,
		Map<String, Object> response) {
	}
	private record KakaoUserInfoDto(
		Long id,
		Map<String, Object> kakao_account,
		Map<String, Object> properties) {
	}

	public void newCustomer(Map<String, Object> userAttributes, Integer providerCode){
		//DB에 회원 저장
		Optional<Customer> customerOptional = customerService.findById(userAttributes.get("id").toString());
		if(customerService.findByEmail(userAttributes.get("email").toString()).isEmpty()){
			if(customerOptional.isEmpty()){
				Customer customer = new Customer();
				customer.setId(userAttributes.get("id").toString());
				customer.setName(userAttributes.get("name").toString());
				customer.setEmail(userAttributes.get("email").toString());
				customer.setPhone(userAttributes.get("phone_number").toString());
				customer.setNickname(userAttributes.get("nickname").toString());
				
				// 생일 변환 (네이버에서 "2025-11-05" 형식으로 받음)
				LocalDate birthday = null;
				if (userAttributes.get("birthyear")!=null && userAttributes.get("birthday")!=null) {
				StringBuffer buf = new StringBuffer(userAttributes.get("birthyear").toString())
				.append("-")
				.append(userAttributes.get("birthday").toString());
				String birthdayObj = buf.toString();
					try {
						// "2025-11-05" 형식의 문자열을 LocalDate로 변환
						birthday = LocalDate.parse(birthdayObj, DateTimeFormatter.ISO_LOCAL_DATE);
						log.info("생일 변환 성공: {}", birthday);
					} catch (DateTimeParseException e) {
						log.warn("생일 파싱 실패: {}. 형식이 올바르지 않습니다.", birthdayObj, e);
						// 만약 네이버에서 다른 형식으로 제공한다면 (예: birthyear + birthday 조합)
						// 여기서 추가 처리 가능
					}
				}
				customer.setBirthday(birthday);
				customer.setProvider(providerCode);
				customer.setJoinDate(LocalDateTime.now());
				customer.setCash(0);
				customer.setStatus(0);
				customer.setTotalPrice(0);
				customer.setPoint(0);
				customer.setRefToken(null);
				customer.setRefTokenUpdatedAt(null);
				customer.setRank("Traveler");
				if(userAttributes.get("gender")!=null){
					if(userAttributes.get("gender").toString().toLowerCase().contains("f")){
						customer.setGender("female");
					}else if(userAttributes.get("gender").toString().toLowerCase().contains("m")){
						customer.setGender("male");
					}
				}


				customerService.save(customer);
			}else if(customerOptional.get().getProvider()==providerCode&&customerOptional.get().getStatus()==1){
				Customer customer = customerOptional.get();
				customer.setStatus(0);
				customerService.save(customer);
			}
		}else{
			// 같은 이메일이지만 다른 provider로 가입된 경우
			log.warn("OAuth2 로그인 실패 - 다른 provider로 가입된 이메일: {}", userAttributes.get("email"));
			throw new OAuth2AuthenticationException("이미 방식으로 가입된 이력이 있는 이메일입니다.");
		}
	}
}
