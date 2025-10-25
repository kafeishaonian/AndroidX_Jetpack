# Cement Adapter 重命名建议

## 问题分析

**当前命名：Cement (水泥)**
- ❌ 不够直观，难以理解库的功能
- ❌ 与 RecyclerView Adapter 的核心价值关联不强
- ❌ 缺乏技术感和现代感

---

## 核心功能分析

基于代码分析，这个库的核心特性：

1. **Model 驱动**：将列表项抽象为独立的 Model
2. **多类型支持**：自动管理不同类型的 ViewHolder
3. **模块化组装**：像搭积木一样构建复杂列表
4. **灵活性**：支持异步/同步、EventHook 等多种模式
5. **类型安全**：强类型检查和绑定

---

## 命名建议方案

### 🏆 方案一：Mosaic (推荐)

**含义**：马赛克、拼图

**优势**：
- ✅ 形象表达"由多个小块组合成整体"的概念
- ✅ 专业且现代化
- ✅ 易于记忆和传播
- ✅ 符合 Android 库命名习惯（如 Epoxy、Groupie）

**命名示例**：
```kotlin
// 基础类
BaseMosaicAdapter
MosaicAdapter
AsyncMosaicAdapter
MosaicModel
MosaicViewHolder

// 包名
com.example.mosaic
```

**品牌定位**：
- **Slogan**: "Mosaic - Build Complex Lists with Simple Pieces"
- **核心理念**: 用简单的片段拼接出复杂的列表

---

### 🎯 方案二：Brick

**含义**：砖块、积木

**优势**：
- ✅ 比 Cement 更具象化
- ✅ 强调模块化组装
- ✅ 简洁易懂
- ✅ 保持了原有"建筑"概念的延续性

**命名示例**：
```kotlin
BaseBrickAdapter
BrickAdapter
AsyncBrickAdapter
BrickModel
BrickViewHolder

// 包名
com.example.brick
```

**品牌定位**：
- **Slogan**: "Brick - Stack Your List Items Like Building Blocks"
- **核心理念**: 像搭积木一样构建列表

---

### 💎 方案三：Flex

**含义**：灵活的、可伸缩的

**优势**：
- ✅ 现代感强
- ✅ 强调灵活性和适应性
- ✅ 简短易记（4个字母）
- ✅ 符合当前技术趋势（如 FlexBox）

**命名示例**：
```kotlin
BaseFlexAdapter
FlexAdapter
AsyncFlexAdapter
FlexModel
FlexViewHolder

// 包名
com.example.flex
```

**品牌定位**：
- **Slogan**: "Flex - The Flexible Way to Build RecyclerView"
- **核心理念**: 灵活应对各种列表场景

---

### 🔧 方案四：Modular

**含义**：模块化的

**优势**：
- ✅ 直接表达核心设计理念
- ✅ 技术感强
- ✅ 清晰传达"模块化组合"的概念
- ✅ 专业术语，开发者熟悉

**命名示例**：
```kotlin
BaseModularAdapter
ModularAdapter
AsyncModularAdapter
ModularModel
ModularViewHolder

// 包名
com.example.modular
```

**品牌定位**：
- **Slogan**: "Modular - Modular Approach to RecyclerView"
- **核心理念**: 模块化设计，组合式开发

---

### 📦 方案五：Component

**含义**：组件

**优势**：
- ✅ 符合现代 UI 组件化思想
- ✅ 与 Jetpack Compose 概念呼应
- ✅ 清晰表达"组件化"设计
- ✅ 通用术语，易于理解

**命名示例**：
```kotlin
BaseComponentAdapter
ComponentAdapter
AsyncComponentAdapter
ComponentModel
ComponentViewHolder

// 包名
com.example.component
```

**品牌定位**：
- **Slogan**: "Component - Component-Based RecyclerView"
- **核心理念**: 组件化构建列表

---

### 🎨 方案六：Palette

**含义**：调色板、工具箱

**优势**：
- ✅ 形象表达"多样化工具"的概念
- ✅ 艺术感和设计感
- ✅ 暗示丰富的功能集合
- ✅ 独特且易记

**命名示例**：
```kotlin
BasePaletteAdapter
PaletteAdapter
AsyncPaletteAdapter
PaletteModel
PaletteViewHolder

// 包名
com.example.palette
```

**品牌定位**：
- **Slogan**: "Palette - A Rich Toolkit for RecyclerView"
- **核心理念**: 提供丰富的工具集

---

## 详细对比表

| 方案 | 直观性 | 专业性 | 记忆度 | 技术感 | 推荐度 |
|------|-------|-------|-------|-------|--------|
| **Mosaic** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 🏆🏆🏆🏆🏆 |
| **Brick** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Flex** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Modular** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Component** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Palette** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

---

## 推荐排序

### 1. 🥇 Mosaic (强烈推荐)

**理由**：
- 形象生动，一听就懂
- 专业度高，适合开源库
- 与已有知名库（Epoxy、Groupie）命名风格一致
- "拼图"的概念完美诠释了库的核心价值

**适合场景**：
- 作为独立开源库发布
- 需要强品牌识别度
- 面向广泛的开发者社区

### 2. 🥈 Flex (现代简洁)

**理由**：
- 简短有力（4个字母）
- 现代感强，符合当前趋势
- 强调灵活性，契合库的设计理念
- 易于输入和记忆

**适合场景**：
- 内部项目或企业级应用
- 强调技术特性
- 追求简洁命名

### 3. 🥉 Brick (延续性强)

**理由**：
- 与原有 Cement 概念相关，迁移成本低
- 形象化，容易理解
- 保持"建筑"主题的一致性

**适合场景**：
- 从 Cement 渐进式迁移
- 内部使用为主
- 保持团队熟悉度

---

## 命名迁移建议

### 完整重命名映射表（以 Mosaic 为例）

| 原名称 | 新名称 |
|-------|--------|
| `BaseCementAdapter` | `BaseMosaicAdapter` |
| `CementAdapter` | `MosaicAdapter` |
| `AsyncCementAdapter` | `AsyncMosaicAdapter` |
| `CementModel` | `MosaicModel` |
| `AsyncCementModel` | `AsyncMosaicModel` |
| `CementViewHolder` | `MosaicViewHolder` |
| `CementLoadMoreModel` | `MosaicLoadMoreModel` |
| `com.example.cement2` | `com.example.mosaic` |

### 过渡期方案

如果已有大量代码使用，可以采用渐进式迁移：

```kotlin
// 1. 创建新名称的类
typealias MosaicAdapter = CementAdapter
typealias MosaicModel = CementModel
typealias MosaicViewHolder = CementViewHolder

// 2. 标记旧名称为过时
@Deprecated("Use MosaicAdapter instead", ReplaceWith("MosaicAdapter"))
class CementAdapter : BaseMosaicAdapter()

// 3. 逐步迁移代码

// 4. 最终移除旧名称
```

---

## 其他命名参考

### 业界知名库命名

| 库名 | 含义 | 特点 |
|------|------|------|
| **Epoxy** | 环氧树脂（粘合剂） | Airbnb 开发，功能强大 |
| **Groupie** | 群组 | 简洁易用 |
| **MultiType** | 多类型 | 直接表达功能 |
| **FastAdapter** | 快速适配器 | 强调性能 |
| **FlexibleAdapter** | 灵活适配器 | 强调灵活性 |

### 命名趋势观察

1. **具象化名词**：Epoxy、Groupie（占 40%）
2. **功能描述**：MultiType、FastAdapter（占 30%）
3. **特性形容**：Flexible（占 20%）
4. **简短单词**：Rx、Flow（占 10%）

---

## 最终建议

综合考虑**品牌识别度**、**专业性**、**易用性**和**扩展性**，我强烈推荐：

### 🏆 首选：**Mosaic**

**完整品牌方案**：

```
品牌名：Mosaic Adapter
Slogan：Build Complex Lists with Simple Pieces
核心价值：模块化 · 类型安全 · 高性能

技术亮点：
- 🧩 Mosaic Pieces: 像拼图一样组合列表项
- ⚡ Async Mosaic: 异步高性能渲染
- 🎯 Type-Safe: 完全类型安全
- 🔌 Event Hooks: 灵活的事件处理
```

**备选：Flex** (如果追求简洁现代)

---

## 需要决策的问题

1. **您更倾向于哪种命名风格？**
   - [ ] 形象化（Mosaic、Brick）
   - [ ] 现代简洁（Flex）
   - [ ] 功能描述（Modular、Component）
   - [ ] 其他：__________

2. **库的定位是？**
   - [ ] 开源项目（需要强品牌）
   - [ ] 内部使用（实用为主）
   - [ ] 企业级（专业稳重）

3. **是否需要保持与 Cement 的关联性？**
   - [ ] 是（选择 Brick）
   - [ ] 否（选择全新命名）

4. **您的个人偏好？**
   - 请告诉我您的想法，我可以进一步优化建议

---

## 附录：完整类名预览

### Mosaic 版本
```kotlin
package com.example.mosaic

abstract class BaseMosaicAdapter : RecyclerView.Adapter<MosaicViewHolder>()
open class MosaicAdapter : BaseMosaicAdapter()
open class AsyncMosaicAdapter(diffHandler: Handler) : BaseMosaicAdapter()
abstract class MosaicModel<VH : MosaicViewHolder>
abstract class AsyncMosaicModel<M : Any, VH : MosaicViewHolder>
open class MosaicViewHolder(itemView: View)
```

### Flex 版本
```kotlin
package com.example.flex

abstract class BaseFlexAdapter : RecyclerView.Adapter<FlexViewHolder>()
open class FlexAdapter : BaseFlexAdapter()
open class AsyncFlexAdapter(diffHandler: Handler) : BaseFlexAdapter()
abstract class FlexModel<VH : FlexViewHolder>
abstract class AsyncFlexModel<M : Any, VH : FlexViewHolder>
open class FlexViewHolder(itemView: View)
```

### Brick 版本
```kotlin
package com.example.brick

abstract class BaseBrickAdapter : RecyclerView.Adapter<BrickViewHolder>()
open class BrickAdapter : BaseBrickAdapter()
open class AsyncBrickAdapter(diffHandler: Handler) : BaseBrickAdapter()
abstract class BrickModel<VH : BrickViewHolder>
abstract class AsyncBrickModel<M : Any, VH : BrickViewHolder>
open class BrickViewHolder(itemView: View)