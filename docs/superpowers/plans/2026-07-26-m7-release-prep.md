# M7: 밸런스 튜닝 · 아이콘 · 출시 준비 구현 계획

**Goal:** 출시 여부를 판단할 수 있는 상태로 만든다.

## Global Constraints

- **서명 키는 저장소에 넣지 않는다.** `keystore.properties`가 있으면 쓰고 없으면 디버그 서명으로
  떨어지게 한다. 키 파일과 비밀번호는 사용자가 직접 관리한다.
- 실기기가 없으므로 **"빌드된다"와 "동작한다"를 구분해 기록한다.**
- 브랜치 `m7-release-prep`.

---

### Task 1: 밸런스 재검증

M1에서 확률표를 확정한 뒤 M4에서 아이템 사용이 배선됐다. 시뮬레이터의 가상 플레이어는
이미 아이템을 쓰지만, 최종 수치를 한 번 더 뽑아 목표 구간 안에 있는지 확인하고
README의 표를 실제 값으로 갱신한다.

### Task 2: 앱 아이콘

M2에서 급히 만든 단순 도형 아이콘을 `SwordArt`의 톤에 맞춰 다시 그린다.
적응형 아이콘 규격(안전 영역 66dp)을 지킨다.

### Task 3: 릴리스 빌드 설정

- `versionName = "1.0.0"`
- `isMinifyEnabled = true`, `isShrinkResources = true` + ProGuard 규칙
- `keystore.properties`가 있으면 릴리스 서명에 사용, 없으면 디버그 서명으로 폴백
- `./gradlew :app:assembleRelease` 가 통과하는지 확인

### Task 4: 출시 체크리스트와 마무리

README에 사용자가 직접 해야 할 일을 적는다 — 키스토어 생성, Play Console 등록,
개인정보 처리방침(이 앱은 수집하는 데이터가 없고 인터넷 권한도 없다).

전체 검증, README v1.0.0, main 병합·푸시.

## 완료 기준

- [ ] 밸런스 리포트가 목표 구간 안이고 README 수치가 실제 값과 일치한다
- [ ] `./gradlew :app:assembleRelease` 가 성공한다
- [ ] 서명 키가 저장소에 없다
- [ ] 출시 전 사용자가 할 일이 README에 적혀 있다
- [ ] 테스트 전부 통과, `main` 푸시
