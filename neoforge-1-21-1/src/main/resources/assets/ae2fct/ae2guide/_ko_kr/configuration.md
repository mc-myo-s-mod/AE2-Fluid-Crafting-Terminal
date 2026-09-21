---
navigation:
  title: 설정
  parent: index.md
  position: 002
categories:
  - ae2fct
---

# 설정

AE2FCT에서는 특정 조합법에서 가상 유체 대신 실제 유체 양동이를 사용하도록 블랙리스트를 지정할 수 있습니다.

## 레시피 블랙리스트

파일 위치: `config/ae2fct-common.toml`

```toml
[recipe_transfer]
virtualFluidRecipeBlacklist = ["immersiveengineering:crafting/redstone_acid"]
```

- `virtualFluidRecipeBlacklist`: 가상 유체 사용을 금지할 조합법 ID 목록입니다.
- 여기에 등록된 조합법은 기존 레시피대로 실제 유체가 담긴 양동이가 있어야 조합할 수 있습니다.
- 멀티플레이에서는 서버의 설정이 우선 적용됩니다.

## 블랙리스트 카테고리

블랙리스트에 등록된 조합법은 레시피 뷰어의 별도 **가상 유체 블랙리스트** 카테고리에서 확인할 수 있습니다.
터미널 유체 상호작용 카드에 레시피 뷰어 사용처 단축키, 보통 `U`, 를 누르면 이 카테고리를 바로 열 수 있습니다.
