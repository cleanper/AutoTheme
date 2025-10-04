| 特性                                            | 传统 JNI / Unsafe                                   | Panama FFM Memory API                           |
|:----------------------------------------------|:--------------------------------------------------|:------------------------------------------------|
| <span style="color:#2E86AB">**安全性**</span>    | <span style="color:#E74C3C">低，容易导致 JVM 崩溃</span>  | <span style="color:#27AE60">高，有边界和类型检查</span>   |
| <span style="color:#2E86AB">**易用性**</span>    | <span style="color:#E74C3C">复杂，需要 C 代码</span>     | <span style="color:#27AE60">简单，纯 Java</span>    |
| <span style="color:#2E86AB">**性能**</span>     | <span style="color:#F39C12">高，但封送开销较大</span>      | <span style="color:#27AE60">高，封送开销极小</span>     |
| <span style="color:#2E86AB">**生命周期管理**</span> | <span style="color:#E74C3C">手动，易出错</span>         | <span style="color:#27AE60">自动（通过 Arena）</span> |
| <span style="color:#2E86AB">**可维护性**</span>   | <span style="color:#E74C3C">低</span>              | <span style="color:#27AE60">高</span>            |

[问题] 为了运行|目前我们需要使用Java22+|且还需要添加额外参数以启用这个实验性功能

      似乎使用后启动速度更慢了[也许是我没有优化]

| **待到后面稳定后，一个全新的分支应该会诞生**

*-希望后面更换时不需要做太多更改-*

简洁地说：<span style="color: #22C55E">[性能]</span><span style="color: #F39C12">></span><span style="color: #E74C3C">[开销]</span> <span style="color: #6B7280">// 两者差距大且这是目前最现代化的方案</span>
