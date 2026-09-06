# 咕咕鸡 (Guguji)

**咕咕鸡** 是一款 Android 基金实时估值追踪应用，帮助用户跟踪自选基金的盘中估算净值与涨跌幅。整体 UI 采用**墨水屏（Kindle）风格**——全灰阶界面，用"墨色浓淡"替代传统红涨绿跌。

- **包名**：`com.fund.guguji`
- **语言**：Java 100%（XML 布局）
- **架构**：MVVM（Room + LiveData + ViewModel）+ RxJava3
- **最低支持**：Android 10（API 29）｜**目标版本**：Android 16（API 36）

## 功能特性

### 一期（当前版本，纯客户端）

| 功能 | 状态 | 说明 |
|------|------|------|
| 🔍 基金搜索 | ✅ | 按代码 / 名称 / 拼音首字母搜索，一键加入自选 |
| 📋 自选基金列表 | ✅ | Room 本地持久化，长按删除 |
| 📊 实时估值展示 | ✅ | 东方财富估值接口，30 秒自动刷新 + 下拉刷新 |
| 📈 涨跌幅展示 | ✅ | 涨 = 浓墨实心徽标，跌 = 淡墨空心徽标，停牌 = 虚线 |
| 🔀 列表排序 | ✅ | 按涨跌幅 / 按添加时间切换 |
| 🗂️ 基金分组 | ✅ | 数据层已实现（分组表 + 多对多关联） |
| 🧹 数据管理 | ✅ | 清空自选数据 |

三个 Tab 结构：

- **自选**——实时估值列表核心页
- **圈子**——二期占位页（邮箱登录 + 朋友持仓分组展示）
- **我的**——清空数据、关于

### 二期（规划中）

邮箱验证码登录（自建 FastAPI 后端）、圈子（朋友持仓互看）、应用内强制更新。详见 [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)。

## UI 设计：墨水屏风格

App 模拟 Kindle 电子墨水屏观感，权威规范见 [design/DESIGN_SPEC.md](design/DESIGN_SPEC.md)，效果图 [design/home_eink.html](design/home_eink.html)（浏览器直接打开可预览）。

设计要点：

- **无彩色**：全灰阶，只有"墨"与"纸"，禁止红绿与偏色
- **墨分五色**：纸底 `#E4E4E4` → 焦墨 `#171717` 五档灰阶表达层级
- **像素硬朗**：1.5px 实线描边、小圆角、无阴影无渐变
- **纸书气质**：标题衬线（思源宋体），数字等宽防抖动
- **克制动效**：仅保留"刷新闪烁"动效，致敬 e-ink 全刷

## 技术栈

| 类别 | 技术 |
|------|------|
| 架构 | MVVM（ViewModel + LiveData + Room），Repository 统一管理本地/远程数据流 |
| 异步 | RxJava 3 + RxAndroid 3 |
| 网络 | OkHttp（直连第三方公开接口） |
| 解析 | Gson（JSON）+ Jsoup（HTML 表格） |
| 数据库 | Room（`guguji_db`） |
| UI | Material Design、RecyclerView、SwipeRefreshLayout、ConstraintLayout |
| 图表 | MPAndroidChart（已引入，图表 UI 待接入） |
| 后台任务 | WorkManager（已引入，Worker 待实现） |
| 构建 | Gradle 8.13 + AGP 8.12.3，Java 17 |

## 数据来源

全部来自公开接口，客户端直连，无自建服务器：

| 数据 | 来源 |
|------|------|
| 实时估值（估算净值/涨跌幅） | 东方财富 `fundgz.1234567.com.cn`（JSONP） |
| 基金搜索 | 东方财富 `fundsuggest.eastmoney.com` |
| 历史净值 / 持仓股票 | 东方财富 `fund.eastmoney.com`（HTML 解析） |
| 股票实时行情 | 腾讯 `qt.gtimg.cn` |

接口字段与返回格式的完整说明见 [API_DOCUMENTATION.md](API_DOCUMENTATION.md)。

## 项目结构

```
app/src/main/java/com/fund/guguji/
├── MainActivity.java              # 主页面（3 Tab 容器，Fragment show/hide 切换）
├── RealTimeFundApp.java           # Application（数据库/仓库单例初始化）
├── data/
│   ├── api/                       # 网络接口层
│   │   ├── EastMoneyApi.java      #   东方财富：估值/净值/持仓/搜索
│   │   ├── FundSearchApi.java     #   基金搜索
│   │   └── TencentQuoteApi.java   #   腾讯行情：股票涨跌幅
│   ├── db/                        # Room 数据库
│   │   ├── AppDatabase.java       #   数据库单例
│   │   ├── dao/                   #   FundDao / GroupDao / ValuationSeriesDao
│   │   └── entity/                #   FundEntity / GroupEntity / GroupFundCrossRef / ValuationPointEntity
│   ├── model/                     # 网络数据模型（非持久化）
│   └── repository/                # FundRepository（远程+本地）/ LocalFundRepository（纯本地）
├── ui/
│   ├── component/                 # BottomTabBar 等自定义控件
│   ├── dialog/                    # ConfirmDialog 确认弹窗
│   ├── fragment/                  # Home（自选）/ Circle（圈子占位）/ Setting（我的）
│   ├── main/                      # FundListAdapter / MainViewModel
│   ├── search/                    # SearchActivity 搜索页
│   └── settings/                  # SettingsActivity 设置页
└── util/                          # Constants / Event / NetworkUtils

design/                            # 设计规范与 HTML 效果图（当前定稿：home_eink.html）
```

## 构建与运行

环境要求：JDK 17、Android Studio（Koala 及以上推荐）

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug

# 单元测试
./gradlew test
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

> 注意：第三方接口均为 HTTP 明文，Manifest 已开启 `usesCleartextTraffic`。接口为非官方公开接口，数据格式可能随上游变更。

## 开发路线

```
一期(已完成)：纯客户端实时估值自选列表
        └──> 二期 A：FastAPI 后端（用户/圈子/持仓/版本）
                  └──> 二期 B：客户端登录 + 圈子 Tab + 强更
                            └──> 二期 C：联调发布
```

完整路线图与验收标准见 [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)。

## 相关文档

- [API_DOCUMENTATION.md](API_DOCUMENTATION.md)——网络接口、数据模型、收益计算与常量配置详解
- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)——产品结论与两期开发计划
- [design/DESIGN_SPEC.md](design/DESIGN_SPEC.md)——墨水屏 UI 设计规范（色板/组件/动效）

## 免责声明

估值数据来自东方财富等公开接口，仅供参考，不构成任何投资建议。本项目为个人学习用途的基金追踪工具，数据准确性以基金公司官方披露为准。
