# MyApplication
![ic_launcher](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)  
테스트용 앱입니다.
## How To Build
다음과 같은 양식으로 카카오 맵 안드로이드 앱 키 값을 입력하여 ./app/src/main/res/values/SecureKey.xml 파일을 생성할 것
```xml
<resources>
    <string name="KakaoMapSecureKey">(카카오맵 안드로이드 앱 키)</string>
    <string name="GoogleAPISecureKey"(Firebase 웹 API 키)</string>
</resources>
```
카카오맵 APP 키 생성 방법은 https://webnautes.tistory.com/m/1319 참조

Firebase 설정에서 SHA 인증서를 등록해야 로그인API가 작동함
SHA-1값 알아내는 방법 https://snowdeer.github.io/android/2017/08/21/android-studio-debug-sha1/
