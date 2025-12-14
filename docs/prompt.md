
# 生成总结文档

@docs/ENHANCE_FUNCTION/NOTIFICATION_IMPLEMENTATION.md 
@docs/ENHANCE_FUNCTION/9-UV_STATISTICS_IMPLEMENTATION.md 
参考这两篇说明文档，结合后端service下新增实现类，如 @src/main/java/com/wait/service/impl/BrowseHistoryServiceImpl.java  @src/main/java/com/wait/service/impl/DelayQueueServiceImpl.java 等
对于每一个功能和service，单独给出一篇文档。简要说明新增加的功能，业界常用方法是什么，这里的实现是什么，使用了Redis中的什么数据和那些命令，实现了什么功能，优缺点等等。不要在文档中放具体代码，只放关键代码的说明。

## 测试prompt

搜索 git diff，为新增的功能编写一些测试代码，充分测试代码的正确性和合理性，包含基础功能测试，压测下的数据一致和其他常见的生产环境测试
数据库中有些mock数据，数据库中有一些mock数据，可以加载进来并测试
暂时将定时任务的刷新时间和定时器的时间调为合适的值，测试定时器和定时任务能否成功执行

运行测试代码，测试功能是否正确。
如果有问题，修改代码，解决问题
给出测试过程和报告
测试完毕后恢复上述定时任务的配置

## 提交代码prompt
检索本次的 git diff，生成一份changelog文档，列出主要修改和新增的功能点，并将代码提交到远端仓库