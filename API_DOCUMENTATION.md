# 咕咕鸡 (Guguji) — 基金实时估值追踪 App 接口文档

## 项目简介

**咕咕鸡** 是一款 Android 基金实时估值追踪应用，帮助用户跟踪自选基金的盘中估算净值、涨跌幅和持仓收益。数据来源于东方财富（East Money）公开接口和腾讯行情接口。

- **包名**: `com.fund.guguji`
- **语言**: Java 100%
- **架构**: MVVM (Room + LiveData + ViewModel) + RxJava3
- **最低 SDK**: Android 10 (API 29)
- **目标 SDK**: Android 16 (API 36)

---

## 一、网络接口层 (API)

项目通过 **OkHttp** 直接调用第三方公开接口，使用 **Jsoup** 解析 HTML、**Gson** 解析 JSON，全部封装为 **RxJava `Observable<T>`** 异步调用。

### 1.1 东方财富实时估值接口 — `EastMoneyApi`

| 方法 | 功能 | 请求地址 | 返回类型 |
|------|------|----------|----------|
| `fetchValuation(fundCode)` | 获取基金实时估值 | `GET http://fundgz.1234567.com.cn/js/{fundCode}.js` | `Observable<FundValuation>` |
| `fetchLatestNav(fundCode)` | 获取最新净值增长率 | `GET http://fund.eastmoney.com/f10/jjjz_{fundCode}.html` | `Observable<NavResult>` |
| `fetchHoldings(fundCode)` | 获取基金持仓股票列表 | `GET http://fund.eastmoney.com/f10/ccmx_{fundCode}.html` | `Observable<HoldingsResult>` |
| `fetchFundHistory(fundCode)` | 获取历史净值数据（全量） | `GET http://fund.eastmoney.com/f10/jjjz_{fundCode}.html` | `Observable<List<NavResult>>` |

#### 1.1.1 `fetchValuation(fundCode)` — 实时估值获取

- **数据源**: 东方财富基金通基金估值接口
- **响应格式**: JSONP（`jsonpgz({...})`），通过正则提取 JSON 体后用 Gson 反序列化
- **返回字段** (`FundValuation`):

| 字段 | JSON 键 | 说明 |
|------|---------|------|
| `fundCode` | `fundcode` | 基金代码 |
| `name` | `name` | 基金名称 |
| `jzrq` | `jzrq` | 净值日期 (YYYY-MM-DD) |
| `dwjz` | `dwjz` | 最近公布的单位净值 |
| `gsz` | `gsz` | 实时估算净值 |
| `gszzl` | `gszzl` | 实时估算涨跌幅 (%) |
| `gztime` | `gztime` | 估值时间 |

#### 1.1.2 `fetchLatestNav(fundCode)` — 最新净值增长率

- **数据源**: 东方财富基金历史净值页面
- **解析方式**: Jsoup 提取 `table#jzzx` 表格前两行
- **返回字段** (`NavResult`):

| 字段 | 说明 |
|------|------|
| `date` | 净值日期 |
| `nav` | 单位净值 |
| `growth` | 日增长率 = (最新净值 - 前日净值) / 前日净值 × 100（保留两位小数） |

#### 1.1.3 `fetchHoldings(fundCode)` — 持仓股票列表

- **数据源**: 东方财富基金持仓页面
- **解析方式**: Jsoup 提取 `table#ccmx` 表格
- **返回字段** (`HoldingsResult`):

| 字段 | 说明 |
|------|------|
| `reportDate` | 报告期（从页面头部提取 YYYY-MM-DD）|
| `stocks` | 持仓股票列表 |

**股票条目** (`HoldingStock`):

| 字段 | 说明 |
|------|------|
| `code` | 股票代码 |
| `name` | 股票名称 |
| `percent` | 占净值比例 (%) |
| `marketValue` | 持仓市值（万元） |

#### 1.1.4 `fetchFundHistory(fundCode)` — 历史净值全量

- **数据源**: 同净值页面，但提取所有行
- **返回**: `List<NavResult>`，每项包含 `date` + `nav`

---

### 1.2 基金搜索接口 — `FundSearchApi`

| 方法 | 功能 | 请求地址 | 返回类型 |
|------|------|----------|----------|
| `searchFunds(keyword)` | 按关键字搜索基金 | `GET https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx?m=1&key={keyword}` | `Observable<List<FundSearchItem>>` |

- **数据源**: 东方财富基金搜索 API
- **请求头**: `User-Agent: Mozilla/5.0`, `Referer: https://fund.eastmoney.com/`
- **响应包装** (`FundSearchResult`):

| 字段 | 说明 |
|------|------|
| `errCode` | 错误码（0 表示成功）|
| `datas` | `List<FundSearchItem>` 搜索结果列表 |

**搜索结果项** (`FundSearchItem`):

| 字段 | 说明 |
|------|------|
| `code` | 基金代码 |
| `name` | 基金名称 |
| `pinyin` | 拼音首字母 |
| `type` | 基金类型 |

---

### 1.3 腾讯行情接口 — `TencentQuoteApi`

| 方法 | 功能 | 请求地址 | 返回类型 |
|------|------|----------|----------|
| `fetchStockChangePercent(marketCode)` | 获取单只股票实时涨跌幅 | `GET http://qt.gtimg.cn/q={marketCode}` | `Observable<Double>` |
| `fetchBatchChangePercent(marketCodes...)` | 批量获取股票涨跌幅 | `GET http://qt.gtimg.cn/q={code1},{code2},...` | `Observable<Double[]>` |

- **数据源**: 腾讯股票行情接口
- **响应格式**: `v_code="...~...~...~涨跌幅~...";`，取索引 5 的字段为涨跌幅百分比
- **市场代码**: 深交所 `sz` 前缀（如 `sz000001`），上交所 `sh` 前缀（如 `sh600519`）

---

## 二、数据仓库层 (Repository)

### 2.1 `FundRepository` — 远程+本地数据协调

| 方法 | 功能 | 说明 |
|------|------|------|
| `getAllFunds()` | 获取所有自选基金（LiveData） | 本地查询 |
| `getAllHoldings()` | 获取所有持仓（LiveData） | 本地查询 |
| `addFund(FundEntity)` | 添加自选基金 | 本地插入 |
| `removeFund(code)` | 删除自选基金（同时删除对应持仓）| 本地删除 |
| `saveHolding(HoldingEntity)` | 保存/更新持仓信息 | 本地插入或更新 |
| `getHolding(fundCode)` | 获取单只基金持仓 | 本地查询 |
| `getFund(code)` | 获取单只基金信息 | 本地查询 |
| `refreshAllValuations()` | **刷新所有基金实时估值 + 计算收益** | 遍历所有基金，逐个调远程 API → 算收益 → 写库 |
| `refreshSingleValuation(FundEntity)` | 刷新单只基金估值 | 远程 API → 算收益 → 写库 |
| `refreshValuationOnly(FundEntity)` | 仅刷新估值（无持仓时不计算收益） | 远程 API → 写库 |
| `fetchHoldingsWithQuotes(fundCode)` | 获取持仓及股票详情 | 远程 API |
| `fetchFundHistory(fundCode)` | 获取基金历史净值 | 远程 API |

### 2.2 `LocalFundRepository` — 纯本地操作

包含基金 CRUD、持仓 CRUD、分组管理功能：

| 方法分类 | 方法 |
|----------|------|
| **基金** | `getAllFunds()`, `getFundByCode()`, `getFundByCodeLive()`, `insertFund()`, `updateFund()`, `deleteFundByCode()`, `getFundCount()` |
| **持仓** | `getAllHoldings()`, `getHoldingByCode()`, `getHoldingByCodeLive()`, `saveHolding()`, `deleteHolding()` |
| **分组** | `getAllGroups()`, `insertGroup()`, `deleteGroupById()`, `addFundToGroup()`, `removeFundFromGroup()`, `getFundCodesInGroup()`, `clearGroup()`, `removeFundFromAllGroups()` |

---

## 三、数据模型 (Model)

### 3.1 本地数据库实体 (Room)

| 表名 | 实体类 | 主要字段 | 说明 |
|------|--------|----------|------|
| `funds` | `FundEntity` | `code`(PK), `name`, `dwjz`(单位净值), `gsz`(估算净值), `gszzl`(估算涨跌幅), `gztime`(估值时间), `jzrq`(净值日期), `profitAmount`(持仓金额), `profitToday`(今日收益), `profitTotal`(总收益), `profitTodayPercent`, `profitTotalPercent` | 自选基金及实时估值 |
| `holdings` | `HoldingEntity` | `fundCode`(PK), `share`(持有份额), `cost`(成本单价) | 用户持仓记录 |
| `fund_groups` | `GroupEntity` | `id`(PK), `name`(分组名) | 基金分组 |
| `group_fund_cross_ref` | `GroupFundCrossRef` | `groupId`, `fundCode` | 分组与基金的多对多关联 |
| `valuation_timeseries` | `ValuationPointEntity` | `id`(auto), `fundCode`, `timestamp`, `value` | 估值时间序列数据（图表用）|

### 3.2 网络数据模型 (非持久化)

| 类 | 来源 | 用途 |
|----|------|------|
| `FundValuation` | 东方财富估值 JSONP | 实时估值展示 |
| `FundSearchResult` + `FundSearchItem` | 东方财富搜索 API | 基金搜索结果 |
| `HoldingsResult` + `HoldingStock` | 东方财富持仓 HTML 解析 | 持仓明细展示 |
| `NavResult` | 东方财富净值 HTML 解析 | 净值历史 |
| `ProfitResult` | `ProfitCalculator` 计算产出 | 收益计算结果 |

### 3.3 收益计算 — `ProfitCalculator.calculate(shares, costPerShare, currentNav, todayGrowth)`

```
持仓金额 = 份额 × 当前估值净值
成本金额 = 份额 × 成本单价
总收益   = 持仓金额 - 成本金额
总收益率 = 总收益 / 成本金额 × 100%
昨日净值 = 当前净值 / (1 + 今日涨跌幅/100)
今日收益 = 份额 × (当前净值 - 昨日净值)
今日收益率 = 今日收益 / 成本金额 × 100%
```

---

## 四、UI 界面

| 界面 | Activity/Fragment | 功能 |
|------|-------------------|------|
| **主页面** | `MainActivity` | 自选基金列表（RecyclerView），下拉刷新，工具栏（搜索/刷新/设置），FAB 添加基金 |
| **搜索页** | `SearchActivity` | 输入关键字搜索基金，展示结果列表，点击添加至自选 |
| **设置页** | `SettingsActivity` | 清除数据、意见反馈（邮件）、关于信息 |
| **编辑持仓弹窗** | `HoldingEditDialog` | 输入持有份额和成本价 |
| **确认弹窗** | `ConfirmDialog` | 通用确认操作弹窗 |

---

## 五、已有功能汇总

| 功能 | 已实现 | 说明 |
|------|--------|------|
| 🔍 **基金搜索** | ✅ | 支持代码/名称/拼音搜索 |
| 📋 **自选基金列表** | ✅ | Room 持久化存储自选基金 |
| 📊 **实时估值展示** | ✅ | 东方财富 fundgz 接口，30 秒自动刷新 |
| 📈 **净值涨跌幅** | ✅ | 实时估算涨跌幅，红涨绿跌 |
| 💰 **持仓收益计算** | ✅ | 今日收益、总收益、收益率 |
| ✏️ **编辑持仓** | ✅ | 设置持有份额和成本价 |
| 📦 **持仓股票查看** | ✅ | 查看基金前十大持仓股票 |
| 🗂️ **基金分组管理** | ✅ | 创建/删除分组，将基金加入/移出分组 |
| 📉 **历史净值** | ✅ | 全量历史净值数据拉取 |
| 📐 **图表支持** | ⚠️ MPAndroidChart 已引入，UI 图表未完整接入 |
| 🔄 **后台刷新** | ⚠️ WorkManager 已引入，Worker 未实现 |
| 📌 **股票实时行情** | ✅ | 腾讯行情接口，单只及批量查询 |
| 🔁 **下拉刷新** | ✅ | SwipeRefreshLayout |
| 📤 **数据导出/备份** | ❌ 未实现 | — |

---

## 六、常量配置 (`Constants.java`)

| 常量 | 值 | 说明 |
|------|-----|------|
| `EAST_MONEY_HOST` | `fundgz.1234567.com.cn` | 估值接口域名 |
| `EAST_MONEY_FUND_HOST` | `fund.eastmoney.com` | 基金数据域名 |
| `TENCENT_QUOTE_HOST` | `qt.gtimg.cn` | 行情接口域名 |
| `CONNECT_TIMEOUT` | 15s | 连接超时 |
| `READ_TIMEOUT` | 15s | 读取超时 |
| `REFRESH_INTERVAL_MS` | 30000ms | 估值自动刷新间隔 |
| `DB_NAME` | `guguji_db` | Room 数据库名 |
| `CHART_DAYS_1M/3M/6M/1Y` | 30/90/180/365 | 图表时间范围预设 |
| `SZ_PREFIX` / `SH_PREFIX` | `sz` / `sh` | 深市/沪市前缀 |

---

## 七、Android 权限

| 权限 | 用途 |
|------|------|
| `INTERNET` | 网络请求 |
| `ACCESS_NETWORK_STATE` | 检查网络连通性 |
| `android:usesCleartextTraffic="true"` | 允许 HTTP 明文请求（第三方接口均为 HTTP）|

---

## 八、技术栈总览

| 类别 | 技术 |
|------|------|
| 语言 | Java |
| 架构 | MVVM (ViewModel + LiveData + Room) |
| 异步 | RxJava 3 + RxAndroid 3 |
| 网络 | OkHttp |
| HTML 解析 | Jsoup |
| JSON 解析 | Gson |
| 数据库 | Room |
| UI 组件 | Material Design, RecyclerView, SwipeRefreshLayout, ConstraintLayout |
| 图表 | MPAndroidChart |
| 图片 | Glide |
| 后台任务 | WorkManager |
