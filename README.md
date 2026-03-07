# RandomRecipe 기능 문서

## 프로젝트에서 제공하는 기능
- 플러그인 활성화 시점(`onEnable`)에 기본 설정 파일(`config.yml`)을 자동 생성합니다.
- 설정값에 따라 플러그인 활성화 로그 메시지를 출력할 수 있습니다.
- 설정값에 따라 플러그인 비활성화 로그 메시지를 출력할 수 있습니다.
- 활성화/비활성화 시 출력되는 메시지를 `config.yml`에서 원하는 문구로 변경할 수 있습니다.

## `config.yml`에서 수정 가능한 항목

### 1) `announce-on-enable`
- 타입: `boolean`
- 기본값: `true`
- 설명: `true`이면 플러그인 활성화 시 콘솔 로그를 출력합니다.

### 2) `announce-on-disable`
- 타입: `boolean`
- 기본값: `true`
- 설명: `true`이면 플러그인 비활성화 시 콘솔 로그를 출력합니다.

### 3) `enable-message`
- 타입: `string`
- 기본값: `"RandomRecipe 플러그인이 활성화되었습니다."`
- 설명: 활성화 로그의 문구를 지정합니다.

### 4) `disable-message`
- 타입: `string`
- 기본값: `"RandomRecipe 플러그인이 비활성화되었습니다."`
- 설명: 비활성화 로그의 문구를 지정합니다.

## 설정 예시
```yml
announce-on-enable: true
announce-on-disable: false
enable-message: "[서버] RandomRecipe 시작"
disable-message: "[서버] RandomRecipe 종료"
```
