# 光伏发电管理平台

基于 [若依（RuoYi）v3.8.6](http://www.ruoyi.vip) 二次开发的光伏发电管理平台，采用前后端分离架构，提供电站监控、数据可视化及完善的后台管理能力。

---

## 技术栈

### 后端

| 技术 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.2.5 |
| MyBatis + PageHelper | 2.1.0 |
| Druid 连接池 | 1.2.21 |
| JWT 鉴权 (jjwt) | 0.12.5 |
| SpringDoc OpenAPI | 2.3.0 |
| Apache POI (Excel) | 5.2.5 |
| Fastjson2 | 2.0.47 |
| Quartz 定时任务 | — |

### 前端

| 技术 | 版本 |
|------|------|
| Vue 3 | 3.2.45 |
| Element Plus | 2.2.27 |
| Vite | 3.2.3 |
| ECharts（数据可视化） | 5.4.0 |
| Three.js（3D 渲染） | 0.183.2 |
| Pinia（状态管理） | 2.0.22 |
| Vue Router | 4.1.4 |
| Axios | 0.27.2 |

---

## 项目结构

```
Pv_web/
├── ruoyi-admin/        # 启动入口 / Web 控制层
├── ruoyi-framework/    # 核心框架（安全、缓存、拦截器等）
├── ruoyi-system/       # 系统基础功能
├── ruoyi-common/       # 通用工具类与常量
├── ruoyi-quartz/       # 定时任务调度
├── ruoyi-generator/    # 代码生成器
├── ruoyi-vue/          # 前端（Vue3 + Vite）
├── sql/                # 数据库初始化脚本
├── doc/                # 项目文档
└── pom.xml             # Maven 父工程配置
```

---

## 核心功能

### 光伏业务
- 电站实时数据监控
- 发电量统计与趋势分析（ECharts）
- 三维可视化展示（Three.js）

### 系统管理
- 用户 / 角色 / 菜单 / 部门 / 岗位管理
- 字典管理 / 参数配置 / 通知公告

### 日志与监控
- 操作日志、登录日志
- 在线用户实时监控
- 服务监控（CPU、内存、磁盘、堆栈）
- 缓存监控与命令统计
- 数据库连接池监视

### 运维工具
- 定时任务（在线增删改查 + 执行日志）
- 代码生成器（Java / HTML / XML / SQL）
- 系统接口文档（SpringDoc OpenAPI）

