# Kastrax教育科技AI解决方案 - 生产环境部署指南

## 📋 概述

本文档提供Kastrax教育科技AI解决方案在生产环境中的完整部署指南，包括系统要求、部署步骤、配置说明和运维指南。

## 🎯 部署目标

根据ed2.md Week 15-16计划，本次部署实现以下目标：
- ✅ 生产环境自动化部署
- ✅ 高可用性和容错能力
- ✅ 监控和日志收集
- ✅ 安全性和数据保护
- ✅ 性能优化和扩展性

## 🔧 系统要求

### 硬件要求
- **CPU**: 最少4核，推荐8核以上
- **内存**: 最少8GB，推荐16GB以上
- **存储**: 最少100GB SSD，推荐500GB以上
- **网络**: 稳定的互联网连接，带宽不少于100Mbps

### 软件要求
- **操作系统**: Ubuntu 20.04+ / CentOS 8+ / RHEL 8+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Git**: 2.25+
- **Curl**: 7.68+

### 网络要求
- **端口开放**:
  - 80 (HTTP)
  - 443 (HTTPS)
  - 8080 (应用服务)
  - 3000 (Grafana监控)
  - 9090 (Prometheus)

## 📦 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Nginx (反向代理/负载均衡)                    │
├─────────────────────────────────────────────────────────────┤
│                  Kastrax EduTech Application                │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL  │    Redis     │  Elasticsearch │  监控服务    │
│   (数据库)    │   (缓存)     │    (搜索)      │ (Prometheus) │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速部署

### 1. 环境准备

```bash
# 克隆项目
git clone https://github.com/your-org/kastrax.git
cd kastrax/kastrax-edutech

# 切换到生产部署分支
git checkout feature/week15-16-user-acceptance-production-deployment

# 进入部署目录
cd deployment/production
```

### 2. 配置环境变量

创建 `.env` 文件：

```bash
# 数据库配置
DB_USERNAME=kastrax_user
DB_PASSWORD=your_secure_password_here
POSTGRES_DB=kastrax_edutech

# Redis配置
REDIS_PASSWORD=your_redis_password_here

# 应用配置
JWT_SECRET=your_jwt_secret_key_here
ENCRYPTION_KEY=your_encryption_key_here

# 监控配置
GRAFANA_PASSWORD=your_grafana_password_here

# 部署配置
VERSION=latest
ENVIRONMENT=production
```

### 3. 执行部署

```bash
# 给部署脚本执行权限
chmod +x ../scripts/deploy-production.sh

# 执行部署
../scripts/deploy-production.sh
```

## 📋 详细部署步骤

### 步骤1: 前置条件检查

部署脚本会自动检查：
- Docker和Docker Compose安装状态
- 环境变量文件存在性
- 网络端口可用性
- 系统资源充足性

### 步骤2: 应用构建

```bash
# 运行单元测试
./gradlew test

# 构建应用
./gradlew build -x test

# 构建Docker镜像
docker build -t kastrax/edutech:latest .
```

### 步骤3: 数据库初始化

```bash
# 启动PostgreSQL
docker-compose up -d postgres

# 执行数据库迁移
docker run --rm --network kastrax-network \
  kastrax/edutech:latest \
  java -jar app.jar --spring.profiles.active=migration
```

### 步骤4: 服务部署

```bash
# 启动所有服务
docker-compose -f docker-compose.prod.yml up -d
```

### 步骤5: 健康检查

```bash
# 检查应用健康状态
curl -f http://localhost:8080/actuator/health

# 检查数据库连接
docker-compose exec postgres pg_isready

# 检查Redis连接
docker-compose exec redis redis-cli ping
```

## 🔧 配置说明

### Nginx配置

位置: `nginx/nginx.conf`

主要配置项：
- 反向代理设置
- SSL/TLS配置
- 负载均衡策略
- 静态资源缓存

### 数据库配置

位置: `init-scripts/init.sql`

包含：
- 数据库初始化脚本
- 用户权限设置
- 索引创建
- 数据迁移脚本

### 监控配置

#### Prometheus配置
位置: `monitoring/prometheus.yml`

监控目标：
- 应用指标 (JVM, HTTP请求)
- 系统指标 (CPU, 内存, 磁盘)
- 数据库指标 (连接数, 查询性能)

#### Grafana配置
位置: `monitoring/grafana/`

包含：
- 预配置的仪表板
- 数据源配置
- 告警规则设置

## 📊 监控和运维

### 访问监控面板

- **Grafana**: http://your-domain:3000
  - 用户名: admin
  - 密码: 在.env文件中配置的GRAFANA_PASSWORD

- **Prometheus**: http://your-domain:9090

### 关键监控指标

#### 应用性能指标
- HTTP请求响应时间
- JVM内存使用率
- 垃圾回收频率
- 线程池状态

#### 系统资源指标
- CPU使用率
- 内存使用率
- 磁盘I/O
- 网络流量

#### 业务指标
- 活跃用户数
- 学习会话数
- API调用频率
- 错误率

### 日志管理

#### 日志收集
使用Filebeat收集以下日志：
- 应用日志 (`/app/logs/`)
- Nginx访问日志
- 系统日志

#### 日志查看
```bash
# 查看应用日志
docker-compose logs -f kastrax-edutech-app

# 查看所有服务日志
docker-compose logs -f

# 查看特定时间段的日志
docker-compose logs --since="2024-12-19T10:00:00" kastrax-edutech-app
```

## 🔒 安全配置

### SSL/TLS配置

1. 获取SSL证书（推荐Let's Encrypt）
2. 将证书文件放置在 `ssl/` 目录
3. 更新Nginx配置启用HTTPS

### 防火墙配置

```bash
# 开放必要端口
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp  # SSH

# 启用防火墙
sudo ufw enable
```

### 数据库安全

- 使用强密码
- 限制数据库访问IP
- 定期备份数据
- 启用连接加密

## 📈 性能优化

### JVM调优

在docker-compose.yml中配置：
```yaml
environment:
  - JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
```

### 数据库优化

PostgreSQL配置优化：
```sql
-- 连接池设置
max_connections = 200
shared_buffers = 256MB
effective_cache_size = 1GB
```

### Redis优化

```bash
# 内存优化
maxmemory 1gb
maxmemory-policy allkeys-lru
```

## 🔄 备份和恢复

### 自动备份

备份脚本会自动执行：
- 数据库备份（每日）
- 应用数据备份（每日）
- 配置文件备份（每周）

### 手动备份

```bash
# 数据库备份
docker-compose exec postgres pg_dump -U kastrax_user kastrax_edutech > backup.sql

# 应用数据备份
docker cp kastrax-edutech-app:/app/data ./backup/app_data
```

### 恢复操作

```bash
# 恢复数据库
docker-compose exec -T postgres psql -U kastrax_user kastrax_edutech < backup.sql

# 恢复应用数据
docker cp ./backup/app_data kastrax-edutech-app:/app/data
```

## 🚨 故障排除

### 常见问题

#### 1. 应用启动失败
```bash
# 检查日志
docker-compose logs kastrax-edutech-app

# 检查配置
docker-compose config
```

#### 2. 数据库连接失败
```bash
# 检查数据库状态
docker-compose ps postgres

# 测试连接
docker-compose exec postgres pg_isready
```

#### 3. 内存不足
```bash
# 检查内存使用
docker stats

# 调整JVM内存设置
# 修改docker-compose.yml中的JAVA_OPTS
```

### 紧急恢复

```bash
# 快速重启所有服务
docker-compose restart

# 从备份恢复
./scripts/restore-from-backup.sh backup_20241219_100000
```

## 📞 支持和联系

如需技术支持，请联系：
- 技术支持邮箱: support@kastrax.ai
- 文档更新: docs@kastrax.ai
- 紧急联系: emergency@kastrax.ai

## 📝 更新日志

### v1.0.0 (2024-12-19)
- ✅ 初始生产环境部署
- ✅ 完整的监控和日志系统
- ✅ 自动化部署脚本
- ✅ 安全配置和备份策略
