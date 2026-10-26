# 1.0.173 소스 복원

이 소스와 리소스는 사용자 제공 `qwertys_homenet-1.0.173.jar`에서 복원했습니다.

- 원본 SHA-256: `02c84c396e29f9946b29f8681c494edc022f8b99883dcff1b1fba16b5f86cd48`
- Minecraft 1.20.1 / Forge 47.3.0 / Java 17
- ForgeAutoRenamingTool 1.0.2로 SRG 이름을 official 1.20.1 이름으로 변환
- Vineflower 1.11.1로 Java 소스 복원
- 복원된 코드의 변수 범위, 람다 캡처, 제네릭 타입을 컴파일 가능한 형태로 보정
- 리소스는 원본 JAR에서 추출하고 빌드 버전을 1.0.173으로 설정

역컴파일은 원래 주석, 서식, 일부 지역 변수명을 복원하지 못합니다. 재빌드한 JAR의 바이트가 원본과 동일함을 의미하지 않습니다. 저장소 루트의 원본 JAR를 기준 자료로 유지합니다.

검증: Java 17에서 `./gradlew build --offline --no-daemon` 통과. 추출한 리소스 1,136개가 원본과 바이트 단위로 일치하고, 이름이 있는 클래스 328개의 public/protected 멤버 서명에 누락이 없음을 확인했습니다. Minecraft 실행 중의 동작은 별도로 검증하지 않았습니다.
