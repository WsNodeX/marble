<div align="center">

# Marble

### 一套零依赖的 Java 分布式系统基础组件

[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Dependencies](https://img.shields.io/badge/dependencies-0-brightgreen)](https://github.com/WsNodeX/marble)
[![Platform](https://img.shields.io/badge/platform-JVM-orange)](https://en.wikipedia.org/wiki/Java_virtual_machine)
[![Tests](https://img.shields.io/badge/tests-43%20passing-success)](https://github.com/WsNodeX/marble)
[![CI](https://github.com/WsNodeX/marble/workflows/CI/badge.svg)](https://github.com/WsNodeX/marble/actions)

**纯 JDK 标准库实现 · 没有任何第三方依赖 · 单文件即可读懂整套思路**

</div>

---

<p align="center">
  <a href="#-为什么需要它">为什么</a> ·
  <a href="#-特性">特性</a> ·
  <a href="#-安装">安装</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-架构">架构</a> ·
  <a href="#-接口一览">接口</a> ·
  <a href="#-路线图">路线图</a> ·
  <a href="#-从零手写分布式组件一篇能发的技术科普文">技术科普文</a>
</p>

---

## ✨ 为什么需要它

分布式系统里有一批"小但绕不开"的组件：全局唯一 ID、判重用的布隆过滤器、做缓存分片的一致性哈希、做限流的令牌桶、以及跨进程互斥的分布式锁。它们在面试里反复出现，在真实架构里又无处不在——但大部分人只是"用过"，没真正写过一遍。

`Marble` 把这几件事用**纯 Java 标准库**全部重写了一遍：不引入任何框架、不依赖任何外部库，每个组件都是几十到两三百行的自洽实现。你可以把它当成一个可读、可测、可改的"实现教科书"——读懂一个，就补齐了一块分布式认知的拼图。

- 想给自己的服务生成**趋势递增、不重复、可反解**的 ID？用 `Snowflake`。
- 想低内存地做"这个值见过没有"的判重？用 `BloomFilter`。
- 想做缓存 / 存储的**平滑分片**，扩缩容只迁移少量数据？用 `ConsistentHash`。
- 想给接口加一道"每秒最多 N 次"的**限流闸门**？用 `TokenBucket`。
- 想理解 Redis `SET NX` 那一套分布式锁到底在防什么？看 `InMemoryLock` 的实现。

## 🚀 特性

- **零依赖**：仅使用 JDK 21 标准库，编译运行都不需要 Maven/Gradle 之外的任何东西。
- **可测试**：所有时间相关的组件都面向 `Clock` 接口，测试里用 `ManualClock` 精确驱动，不靠 `Thread.sleep` 凑覆盖率。
- **自洽实现**：布隆过滤器的哈希、一致性哈希的环、雪花算法的位运算，全部自己写，逻辑透明。
- **生产可演进**：接口设计贴近真实方案（如分布式锁的 token 所有权 + 租约过期），换成 Redis/etcd 后端时调用方式不变。
- **覆盖完整**：43 个单元测试全绿，CI 在 JDK 21 上自动跑。

## 📦 安装

### 方式一：直接复制源码

整个工具箱就是 `src/main/java/marble` 下的几个包，把对应包拷进你的项目即可，没有任何传递依赖。

### 方式二：本地编译

```bash
# 需要 JDK 21
./build.sh          # Linux / macOS
build.bat           # Windows
```

脚本会编译 `src/main` 与 `src/test`，运行演示程序，并执行全部单元测试。

### 方式三：作为依赖

把 `build/main` 下的 class 打成 jar 后引入，或直接将 `marble` 包发布到你的私有仓库。

## ⚡ 快速开始

```java
import marble.id.Snowflake;
import marble.id.SnowflakeIdGenerator;
import marble.bloom.BloomFilter;

public class QuickStart {
    public static void main(String[] args) {
        // 1) 生成分布式唯一 ID
        SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(Snowflake.classic(), 1);
        long id = idGen.nextId();
        System.out.println("id=" + id + " -> " + idGen.decode(id));

        // 2) 用布隆过滤器判重
        BloomFilter filter = new BloomFilter(1_000, 0.01);
        filter.put("order-1001");
        System.out.println(filter.mightContain("order-1001")); // true
        System.out.println(filter.mightContain("order-9999")); // 极大概率为 false
    }
}
```

也可以直接运行内置演示：

```bash
java -cp build/main marble.examples.Demo
```

预期输出（ID 数值随运行时间变化，其余稳定）：

```
== Snowflake distributed id ==
id=394849180679577600  node=42  seq=0
id=394849180734103552  node=42  seq=0
id=394849180734103553  node=42  seq=1

== Bloom filter ==
contains banana? true
contains zucchini? false
bit size=9586 hashes=7

== Consistent hashing ==
key 'user:1001' -> cache-c
key 'order:77'   -> cache-c
after removing cache-b, key 'order:77' -> cache-c

== Token-bucket rate limiter ==
allowed 5 of 8 immediate requests (capacity=5)

== Distributed lock ==
acquired? true  locked? true
released? true  locked? false
```

## 🏗️ 架构

```
                         Marble  (零依赖 Java 分布式基础组件)
┌──────────────────────────────────────────────────────────────────┐
│  marble.core   位运算 Bits · 确定性哈希 Hash · 时间源 Clock · 断言   │
├──────────────────────────────────────────────────────────────────┤
│  marble.id        SnowflakeIdGenerator  —— 趋势递增的分布式唯一 ID  │
│  marble.bloom     BloomFilter / CountingBloomFilter —— 概率判重     │
│  marble.hash      ConsistentHash        —— 带虚拟节点的一致性哈希环 │
│  marble.ratelimit TokenBucket           —— 令牌桶限流              │
│  marble.lock      InMemoryLock          —— 分布式锁（本地参考实现）  │
├──────────────────────────────────────────────────────────────────┤
│  marble.examples  Demo   —— 一行不落的端到端演示                    │
│  marble.test      *Test  —— 43 个 JUnit 5 单元测试                 │
└──────────────────────────────────────────────────────────────────┘
```

每一层都只依赖下一层，没有任何环依赖；`core` 是所有组件的共同底座。

## 🔌 接口一览

| 组件 | 关键类型 | 核心方法 | 说明 |
| --- | --- | --- | --- |
| 分布式 ID | `SnowflakeIdGenerator` | `nextId()` / `decode(long)` | 64 位雪花 ID，可反解出时间戳、节点、序列号 |
| 布隆过滤器 | `BloomFilter` | `put` / `mightContain` / `merge` / `toBase64` | 空间高效的概率集合，无假阴性 |
| 计数布隆 | `CountingBloomFilter` | `put` / `remove` / `mightContain` | 支持删除的计数型变体 |
| 一致性哈希 | `ConsistentHash<T>` | `addNode` / `removeNode` / `getNode` | 虚拟节点，扩缩容仅迁移少量 key |
| 令牌桶 | `TokenBucket` | `tryAcquire()` / `tryAcquire(int)` | 惰性补充令牌，不阻塞 |
| 分布式锁 | `InMemoryLock` | `tryLock` / `unlock` / `isLocked` | token 所有权 + 租约过期，接口对齐 Redis 方案 |

所有"时间相关"的组件（`SnowflakeIdGenerator`、`TokenBucket`、`InMemoryLock`）都接收一个 `Clock` 参数，因此既能在生产用系统时钟，也能在测试里用 `Clock.ManualClock` 手动拨动时间。

## 🧭 路线图

- [ ] 增加 `RedissonLock` / `EtcdLock` 作为 `DistributedLock` 的网络后端
- [ ] 为 `BloomFilter` 提供 `approximateCount()`（基于 bit 饱和度的基数估计）
- [ ] 为一致性哈希增加带权虚拟节点（异构机器权重）
- [ ] 增加 `RateLimiter` 的滑动窗口 / 漏桶实现并做对比
- [ ] 提供 `marble-bench` 微型基准，给出各组件吞吐与内存占用
- [ ] 增加 `SegmentedLock`：分段锁，演示如何把热点打散

## 🤝 贡献

欢迎以 issue / PR 的形式补充新的分布式基础组件，或改进现有实现的性能与可读性。提交前请保证 `./build.sh` 全绿。

## 📄 License

[MIT](LICENSE) © WsNodeX

---

# 从零手写分布式组件：一篇能发的技术科普文

> 下面这一段是写给想把它改写成发布稿的朋友的"文章底稿"。它讲清了每个组件解决的问题、核心原理和关键实现点，你可以直接在这个骨架上润色、加图、补案例，发到掘金 / 知乎 / 小红书 / B 站。

## 一、为什么要手写这些"轮子"

面试造火箭，工作拧螺丝——这句话在分布式领域尤其扎心。你每天都在用雪花 ID、布隆过滤器、一致性哈希，但一旦被问"它内部长什么样"，很多人只能说出名词。

真正把这些组件**手写一遍**的价值在于：你会理解"为什么这么设计"，而不只是"怎么调用"。比如：

- 雪花 ID 为什么要留 41 位时间戳？因为 2^41 毫秒 ≈ 69 年，足够用到退休。
- 布隆过滤器为什么"有假阳性、无假阴性"？因为它是用比特位"投票"的，位被别人占了就可能误判，但某一位没被占就一定没来过。
- 一致性哈希为什么加"虚拟节点"？因为裸的哈希取模在加节点时会让几乎全部 key 重新映射，而虚拟节点把迁移量摊薄到约 1/N。

这篇文章就带你用纯 Java（不依赖任何库）把这五个组件各写一遍，全部代码在 `Marble` 项目里，可读、可测、可改。

## 二、五个组件的核心原理

### 1. 雪花 ID：把"时间 + 机器 + 序号"压进一个 long

一个 long 是 64 位。雪花算法这样切：

```
1 位符号 | 41 位时间戳 | N 位机器号 | M 位序列号   （N + M ≤ 22）
```

同一毫秒内靠序列号区分；不同毫秒时间戳自然递增。反解时只要按位 `>>>` 切回去就能拿到时间戳、机器号、序号。最关键的细节是**时钟回拨**：如果系统时间往回跳，继续发号会产生重复，所以实现里直接抛异常而不是"将错就错"——宁可拒发，也不能发重。

### 2. 布隆过滤器：用比特位做"见过的集合"

它本质是一串 bit 位 + k 个独立哈希。写入时把 k 个哈希位置 1；查询时看这 k 位是否全 1。全 1 不代表一定来过（假阳性），但只要有 0 就一定没来过（无假阴性）。bit 数组大小和哈希个数由"预期元素量"和"可接受的误判率"公式直接算出，不需要拍脑袋。想支持删除？把 bit 换成计数器，就是计数布隆过滤器。

### 3. 一致性哈希：让扩缩容只动一小撮数据

把 hash 空间想成一个环，每个物理节点放 N 个"虚拟节点"上去。一个 key 顺时针找到最近的虚拟节点，就算它负责。这样加一台机器，只会"抢走"原本属于相邻节点的那一小段弧，而不是把全体 key 重新洗牌——这正是 Memcached、Redis Cluster 做分片的底层逻辑。

### 4. 令牌桶：限流的教科书方案

维护一个容量固定的桶，按固定速率往里加令牌，每次请求消耗一枚。令牌够就放行，不够就拒绝。关键点是**惰性补充**：只在被问到时才按"当前时间 - 上次时间"补令牌，空闲时不浪费任何计算。它只限"速率"，不限"突发"——桶是满的时允许一次性把存量令牌花光，这正是限流想要的行为。

### 5. 分布式锁：token 所有权 + 租约过期

单机锁靠 JVM 管，跨进程锁得靠一个所有节点都看得见的中间件。最经典的形态是 Redis 的 `SET key token NX PX lease`：用唯一 token 标明"这把锁归我"，用过期时间防止我崩了锁永远不释放。释放时必须"只删自己的 token"，否则会误删别人的锁。`Marble` 的 `InMemoryLock` 把这套语义在单机里完整演示了一遍，将来换成 Redis 后端时，接口完全一致。

## 三、关键实现点（面试常问）

1. **雪花 ID 的并发安全**：用 `synchronized` 把"取时间 + 自增序列 + 拼装"三步包成原子，同一毫秒序列用尽就自旋等下一毫秒。
2. **布隆过滤器的哈希效率**：不要真跑 k 次哈希；用两个基哈希做 `h_i = h1 + i*h2` 推导出 k 个，省掉多次扫描数据。
3. **一致性哈希的平衡性**：直接用 32 位哈希放点会肉眼可见地扎堆，所以 `Marble` 用两个 Murmur3 种子拼成 64 位哈希，虚拟节点越多越均匀。
4. **限流的确定性测试**：把所有"现在时间"收口到 `Clock` 接口，测试用 `ManualClock` 手动拨时间，避免 `Thread.sleep` 带来的抖动和慢。

## 四、和"直接用现成库"的对比

| 维度 | 直接用现成库 | 手写 / 用 Marble |
| --- | --- | --- |
| 上手速度 | 快 | 慢一点 |
| 理解深度 | 浅，黑盒 | 深，白盒可读 |
| 可控性 | 受制于人 | 想改就改 |
| 依赖性 | 可能引入传递依赖 | 零依赖 |
| 面试叙事 | "我用过" | "我写过，原理是…" |

结论不是"不要用库"，而是**至少亲手写过一遍**。库是给别人用的，原理是自己的。

## 五、怎么把它扩展成一篇发布稿

- **加图**：把雪花 ID 的位布局、哈希环、令牌桶画成图，可读性立刻翻倍。
- **加场景**：每个组件配一个"我项目中真遇到过的问题"小故事。
- **加 benchmark**：跑一组数字，比如"布隆过滤器存 100 万条只占 X MB，误判率 Y%"。
- **加对比实验**：一致性哈希 N 从 50 加到 200，迁移比例从多少降到多少。
- **收尾升华**：点到"基础组件背后是分布式系统的trade-off思维"，比单纯贴代码更有传播力。

> 发布时记得把项目链接和"零依赖、可运行 demo"作为钩子，读者能立刻 clone 跑起来，收藏率会明显更高。
