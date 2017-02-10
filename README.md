## Statistics
- 一个用于统计Android手机应用使用情况的APP
- 不是采用后台服务跟踪统计的方式
- Google 官方在 android API 21 版本以后提供了一个 android.app.usage 的 API用于用户获取手机中各个应用的使用情况以及使用时间该
- 应用通过该官方API获取到应用的使用时间及使用次数
- 获取时间时由于没有调用后台服务实行精确统计，所以在时间节点上可能显示不很精确
- 图表的绘制绘制图表调用的库来自于开源的 MPAndroidChart 图表项目，其项目位置：https://github.com/PhilJay/MPAndroidChart
