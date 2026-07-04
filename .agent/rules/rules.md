---
trigger: always_on
---

# 估估基 (real-time-fund-guguji) 开发与交互规范

这里定义了本项目的核心开发规范、设计原则以及 AI 助手交互的约束条件。每次执行任务前请务必遵循。

## 1. 交互与行为准则
- **确认与设计**：涉及重大架构调整或 UI 布局重构时，优先与用户对齐设计思路。
- **无占位符**：不编写 `TODO` 或临时占位代码。
- **代码完整性**：在修改已有代码时，保留与当前修改无关的注释、文档，不得随意删减。
- **注释使用中文**：每次修改使用到的注释使用中文。

## 2. 技术栈与架构规范
- **开发语言**：Java（业务逻辑）与 XML（布局设计）。
- **设计规范**：使用 Material Design 规范进行 UI 开发。
- **架构模式**：采用 MVVM (Model-View-ViewModel) 模式。
  - 数据源统一由 Repository 管理，支持本地/网络双重数据流。
  - LiveData / StateFlow 用于 ViewModel 与 Activity/Fragment 的生命周期安全通信。
- **异步处理**：统一使用 Kotlin Coroutines 进行异步操作。

## 3. UI/UX 与设计美学
- **视觉品质**：
  - 避免使用单一饱和度的红、绿、蓝，应使用和谐的配色方案（如基于主题色的渐变或 Material Color 调色板）。
  - 支持深色模式（Dark Mode），保证背景与文字对比度。
- **动画与交互**：
  - 合理添加微交互与过渡动画（如 SwipeRefresh 刷新动画、点击缩放、加载骨架屏等），增强交互连贯性。
  - 点击态和焦点态必须清晰，支持 Ripple 涟漪效果。
- **屏幕适配**：
  - 布局应具备良好的响应式设计，合理使用 `ConstraintLayout`。
  - 间距、大小统一使用 `dp`/`sp`，不可硬编码尺寸。

## 4. 文件与资源管理
- **文件命名**：
  - 布局文件：`fragment_xxx.xml`、`activity_xxx.xml`、`item_xxx.xml`。
  - 资源 ID：遵循蛇形/驼峰命名规则并保持风格一致（如 `tv_title` / `tvTitle`, `btn_submit` / `btnSubmit`, `fab_add` / `fabAdd`）。
- **国际化**：字符串资源统一管理，尽量定义在 `res/values/strings.xml` 中，避免在布局或代码中硬编码字面量。
