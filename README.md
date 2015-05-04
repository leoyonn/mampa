# mampa
## xiaoMi Asynchronous Message Processing Architecture.

## 使用方法
### 步骤
  1. define your FSM with RuleSet: State(S) x Event(E) -> Actions (A), State(S')
  2. define your actor extends Actor, using ActorGroup for multi-threading;
  3. use actorGroup.tell(Event) to submit an async job.

### 参见示例
  1. src/main/java/me/lyso/mampa/demo/ActorDemoV4.java
  2. src/main/java/me/lyso/mampa/demo/ActorDemoV3.java
  3. src/main/java/me/lyso/mampa/demo/ActorDemoV2.java
  4. src/main/java/me/lyso/mampa/demo/ActorDemo.java


## 版本升级log

### V0.2.0
  1. tellAfter/tellTimeout接口返回Timeout
  2. 支持timeout在状态切换时自动cancel

### V0.1.9
  1. 支持一个消息batch个多个actor中的多个fsm-target（如果target不存在则忽略）；

### V0.1.8
  1. 支持match-all-actor（即一个消息可发给一个ActorGroup中的所有Actor）；

### V0.1.7
  1. 支持多优先级队列；
  2. 添加更多perf-counter及异常handle；

### V0.1.6
  1. 支持一个mampa中有多种rule-set；
  2. 优化接口逻辑，将IActor分化为IActorGroup/IActor，tell接口中将target拆分；
  3. testcases for multi-rule-set；
  4. 修复AbstractRuleSet.handle中找action的一个bug；

####  接口主要有两处变化
  1. 最外部的接口改名为IActorGroup，tell参数中的target分化为三个：(K1 actorTarget, IFsmType fsmType, K2 fsmTarget)，内部接口IActorGroup只需要两个（IFsmType fsmType, K2 fsmTarget)；
  2. AbstractRuleSet构造函数变更，需要传入FsmType，ActorGroup/Actor构造函数中可传入1个或多个AbstractRuleSet；


## V0.1.5
  1. IAction接口抛出exception；

## V0.1.4
  1. 使用object-pool优化actor中的fsm使用；

## V0.1.3
  1. 增加可以匹配所有fsm的fsmRouter接口

## V0.1.2 ##
  增加了如下两种addRule：
  1. 某种state对应所有event；
  2. 某种event对应所有state；

  从此，大家可以方便的添加三个级别的rules：
  level1：指定state+event；
  level2.1：仅指定state，则能handle这个state对应的所有event；
  level2.2：仅指定event，则能handle这个event对应所有的state情形；
  levle3：default-rule：以上rule都找不到时的规则。

  每来一个事件，FSM会根据当前state和etype按level从上到下顺序查找合适的rule。耶！

## V0.1.1 ##
  1. 一些bugfix及exception-handle；

## V0.1.0 ##
  1. Refactor timeout.
  2. Add init hook, stop state.
  3. Demo V2 updated.

## V0.0.3.a ##
  1. 提供FSM、Actor用不同的方式路由的能力；
  2. 修复Timeout的逻辑；
  3. 提供销毁FSM的能力；

## V0.0.3 改进： ##
  1. ThreadPool放回到Actor内部；
  2. 取消ShutdownHook；
  3. State里添加自定义数据，可以根据自定义数据扩展出更丰富的state跳转和action；
  4. 完善Timeout的流程：timeout-action执行时会判断触发时的state type是否变更；
  5. 精简参数及流程，对应用层隐藏更多细节；
  6. 完善所有java-doc；
  7. 一个新的demo-码农的生活单调版；

## V0.0.2 改进： ##
  1. 增加ActorGroup，Thread初始化时创建好，每个Thread负责一个Actor的Disruptor，由IActorRouter负责消息路由到哪个Actor；
  2. 将Actors类简化、静态化（->ActorGroups）；
  3. 优化向Disruptorpublish时总是new EventTranslator（不构造小对象）；
  4. 优化IAction中传入的Event为不需要构造新对象；
  5. 较少泛型类型，增加代码的可读性/可维护性；
  6. Timer；
  7. 一个有意思的demo；


## TODO ##
  1. Timer有个bug，即timer即不分状态，又只有一个Timeout类型，导致无法区分当前收到的timeout是不是当前状态导致的；
  2. Actor的mailbox(Disruptor)增加优先级队列（可能需要自定义EventProcessor）；
  3. Reuse FSM in Actor;
  4. TimeoutAction.handle: 是否可以不new TimerTask？
