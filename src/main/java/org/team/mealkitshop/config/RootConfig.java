package org.team.mealkitshop.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 환경설정용 이라고 스프링에게 알려준다.
public class RootConfig {

    @Bean // 환경설정용 객체로 지정
    public ModelMapper getMapper(){
        //  implementation 'org.modelmapper:modelmapper:3.1.0'
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.LOOSE); //  P560 BNO 버그시 수정

        // https://devwithpug.github.io/java/java-modelmapper/

        //MatchingStrategies.STANDARD(default)
        //모든 destination 객체의 property 토큰들은 매칭 되어야 한다.
        //모든 source 객체의 property들은 하나 이상의 토큰이 매칭되어야 한다.
        //토큰은 어떤 순서로든 일치될 수 있다.
        //MatchingStrategies.STRICT
        //가장 엄격한 전략
        //source와 destination의 타입과 필드명이 같을 때만 변환
        //의도하지 않은 매핑이 일어나는 것을 방지할 때 사용
        //MatchingStrategies.LOOSE
        //가장 느슨한 전략
        //토큰을 어떤 순서로도 일치 시킬 수 있다.
        //마지막 destination 필드명은 모든 토큰이 일치해야 한다.
        //마지막 source 필드명에는 일치하는 토큰이 하나 이상 있어야 한다.

        return modelMapper;
        // 엔티티를 dto로 변환하게 환경설정함!!
        //https://velog.io/@hjhearts/SpringModelMapper-ModelMapper%EC%82%AC%EC%9A%A9%EB%B2%95-%EC%B4%9D%EC%A0%95%EB%A6%AC

    }
}