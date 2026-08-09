# 강화 화면 디자인 QA

## 비교 대상

- source visual truth: `C:\workAndroid\SwordForge\build\visual-audit\before\02-forge-loaded.png`
- implementation screenshot: `C:\workAndroid\SwordForge\build\visual-audit\phone-scroll-fix\08-final-phone.png`
- advanced-state screenshot: `C:\workAndroid\SwordForge\build\visual-audit\phone-scroll-fix\07-advanced-fixed-phone.png`
- full-view comparison: `C:\workAndroid\SwordForge\build\visual-audit\phone-scroll-fix\10-reference-final-comparison.png`
- source viewport: 1080×2280 px, Android 에뮬레이터, +0 상태
- implementation viewport: 1080×2340 px, SM_A165N, 물리 밀도 450 dpi, 글꼴 배율 1.0, +0 상태
- CSS size: 해당 없음. Jetpack Compose 네이티브 화면이다.
- density normalization: 시스템 상태 표시줄과 내비게이션 바를 제외했다. 소스 앱 영역은
  `(0, 52)–(1080, 2184)`의 1080×2132 px, 구현 앱 영역은 `(0, 88)–(1080, 2162)`의
  1080×2074 px로 잘랐다. 구현 영역을 1080×2132 px로 맞춘 뒤 한 비교 이미지에 나란히 배치했다.
- state: 기본 +0 화면을 시각 기준으로 비교하고, +16 용검·사냥터 해금 상태를 별도 반응형
  스트레스 상태로 확인했다.

## 전체 화면 비교

기존 화면의 정보 순서인 최고 기록 → 재화 → 검 → 이름과 단계 → 확률과 비용 → 소모품 →
강화 버튼 → 다섯 메뉴가 유지됐다. 추가된 대장간 배경은 검 스프라이트 뒤에 낮은 투명도로
놓였고, 검 디자인과 픽셀 렌더링은 바뀌지 않았다. 최종 +0 화면과 +16 화면 모두 핵심 조작이
앱 영역 안에 들어온다.

집중 영역 비교는 별도로 만들지 않았다. 이번 수정의 차단 조건은 화면 전체의 세로 배치와
하단 메뉴 노출 여부였고, 1080 px 원본 캡처에서 글자·아이콘·검 픽셀이 충분히 읽혔다.

## 필수 표면 점검

- 글꼴과 타이포그래피: 이전 화면의 기본 글꼴, 굵기, 크기, 줄바꿈을 복원했다. +16의 긴 스킬과
  파괴 안내도 한 줄로 유지된다.
- 간격과 레이아웃: 세로 스크롤과 고정 도크를 제거했다. 검 영역만 남는 높이를 사용하며,
  +16에서 사냥터와 다섯 메뉴가 모두 보인다.
- 색과 토큰: 이전 어두운 테마와 금색 강화 버튼을 유지했다. 배경 위에는 위아래 암막을 더해
  텍스트 대비가 떨어지지 않게 했다.
- 이미지 품질: 검 리소스는 변경하지 않았다. `forge_hall_background.png`는 `drawable-nodpi`에서
  비율을 유지한 채 화면을 채우고, 픽셀 검은 기존 렌더링을 사용한다.
- 문구와 콘텐츠: 기존 강화 정보는 유지했다. 고단계 강화석 요구와 차단 이유만 강화 버튼의
  보조 문구로 합쳐 중복 세로 공간을 없앴다.

## 비교 이력

1. 첫 캡처 `02-current-phone-clear.png`: P1. 상단 기록과 재화가 화면 밖으로 밀리고 왼쪽에
   스크롤 표시가 보였다.
2. 1차 복원 `03-fixed-phone.png`: 기본 +0 화면은 한 화면에 들어왔다. 고단계 재현
   `06-advanced-fixed-phone.png`에서는 P1으로 하단 메뉴 일부가 잘렸다.
3. 수정: 고단계 강화석 안내를 강화 버튼 안으로 합치고, 소모품 중복 설명과 하단 간격을 줄였다.
4. 2차 확인 `07-advanced-fixed-phone.png`: +16, 강화석, 사냥터가 모두 표시된 상태에서 다섯 메뉴가
   전부 보였다.
5. 최종 스와이프 확인: `08-final-phone.png`과 `09-final-after-swipe.png`의 앱 영역
   `(0, 88)–(1080, 2162)`은 변경 픽셀이 0개였다. 전체 이미지 차이는 시스템 상태 표시줄의
   배터리 아이콘 21×12 px 영역뿐이었다.

## 남은 항목

- P0/P1/P2 없음.
- P3: 배경 때문에 재화와 메뉴의 기존 표면이 이전 단색 화면보다 조금 더 분리되어 보인다.
  이는 배경 위 가독성을 위한 의도된 차이다.

final result: passed
