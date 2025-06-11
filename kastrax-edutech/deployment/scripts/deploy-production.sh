#!/bin/bash

# Kastrax教育科技AI解决方案 - 生产环境部署脚本
# 根据ed2.md Week 15-16计划实施的生产环境部署自动化

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOYMENT_DIR="$PROJECT_ROOT/deployment"
PRODUCTION_DIR="$DEPLOYMENT_DIR/production"
VERSION=${VERSION:-"latest"}
ENVIRONMENT=${ENVIRONMENT:-"production"}

# 检查必要的工具
check_prerequisites() {
    log_info "检查部署前置条件..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    # 检查环境变量文件
    if [ ! -f "$PRODUCTION_DIR/.env" ]; then
        log_error "环境变量文件 .env 不存在，请创建并配置必要的环境变量"
        exit 1
    fi
    
    log_success "前置条件检查通过"
}

# 构建应用镜像
build_application() {
    log_info "构建Kastrax教育科技应用镜像..."
    
    cd "$PROJECT_ROOT"
    
    # 运行测试
    log_info "运行单元测试..."
    ./gradlew test
    
    # 构建应用
    log_info "构建应用..."
    ./gradlew build -x test
    
    # 构建Docker镜像
    log_info "构建Docker镜像..."
    docker build -t "kastrax/edutech:$VERSION" -f "$DEPLOYMENT_DIR/Dockerfile" .
    
    # 标记为latest
    if [ "$VERSION" != "latest" ]; then
        docker tag "kastrax/edutech:$VERSION" "kastrax/edutech:latest"
    fi
    
    log_success "应用镜像构建完成"
}

# 准备部署环境
prepare_deployment() {
    log_info "准备部署环境..."
    
    cd "$PRODUCTION_DIR"
    
    # 创建必要的目录
    mkdir -p logs data ssl backup
    mkdir -p nginx/conf.d monitoring/grafana/{dashboards,datasources}
    
    # 设置权限
    chmod 755 logs data backup
    
    # 复制配置文件
    if [ ! -f "nginx/nginx.conf" ]; then
        log_info "复制Nginx配置文件..."
        cp "$DEPLOYMENT_DIR/nginx/nginx.conf" nginx/
        cp "$DEPLOYMENT_DIR/nginx/conf.d/"* nginx/conf.d/
    fi
    
    if [ ! -f "monitoring/prometheus.yml" ]; then
        log_info "复制监控配置文件..."
        cp "$DEPLOYMENT_DIR/monitoring/"* monitoring/
    fi
    
    log_success "部署环境准备完成"
}

# 数据库迁移
migrate_database() {
    log_info "执行数据库迁移..."
    
    # 启动数据库服务
    docker-compose -f docker-compose.prod.yml up -d postgres
    
    # 等待数据库启动
    log_info "等待数据库启动..."
    sleep 30
    
    # 执行数据库迁移
    docker run --rm --network kastrax-network \
        -e DATABASE_URL="jdbc:postgresql://postgres:5432/kastrax_edutech" \
        -e DATABASE_USERNAME="$DB_USERNAME" \
        -e DATABASE_PASSWORD="$DB_PASSWORD" \
        "kastrax/edutech:$VERSION" \
        java -jar app.jar --spring.profiles.active=migration
    
    log_success "数据库迁移完成"
}

# 部署服务
deploy_services() {
    log_info "部署生产环境服务..."
    
    cd "$PRODUCTION_DIR"
    
    # 加载环境变量
    export $(cat .env | xargs)
    export VERSION="$VERSION"
    
    # 停止现有服务
    log_info "停止现有服务..."
    docker-compose -f docker-compose.prod.yml down
    
    # 启动所有服务
    log_info "启动生产环境服务..."
    docker-compose -f docker-compose.prod.yml up -d
    
    log_success "服务部署完成"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_info "健康检查尝试 $attempt/$max_attempts..."
        
        # 检查应用健康状态
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "应用健康检查通过"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            log_error "健康检查失败，部署可能存在问题"
            return 1
        fi
        
        sleep 10
        ((attempt++))
    done
    
    # 检查其他服务
    log_info "检查数据库连接..."
    if docker-compose -f docker-compose.prod.yml exec -T postgres pg_isready -U "$DB_USERNAME" -d kastrax_edutech; then
        log_success "数据库连接正常"
    else
        log_error "数据库连接失败"
        return 1
    fi
    
    log_info "检查Redis连接..."
    if docker-compose -f docker-compose.prod.yml exec -T redis redis-cli ping | grep -q PONG; then
        log_success "Redis连接正常"
    else
        log_error "Redis连接失败"
        return 1
    fi
    
    log_success "所有健康检查通过"
}

# 运行验收测试
run_acceptance_tests() {
    log_info "运行生产环境验收测试..."
    
    # 等待服务完全启动
    sleep 60
    
    # 运行验收测试
    cd "$PROJECT_ROOT"
    ./gradlew test --tests "*UserAcceptanceTestFramework*" \
        -Dtest.environment=production \
        -Dtest.base.url=http://localhost:8080
    
    if [ $? -eq 0 ]; then
        log_success "验收测试通过"
    else
        log_error "验收测试失败"
        return 1
    fi
}

# 创建备份
create_backup() {
    log_info "创建部署前备份..."
    
    local backup_timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_dir="$PRODUCTION_DIR/backup/backup_$backup_timestamp"
    
    mkdir -p "$backup_dir"
    
    # 备份数据库
    docker-compose -f docker-compose.prod.yml exec -T postgres \
        pg_dump -U "$DB_USERNAME" kastrax_edutech > "$backup_dir/database.sql"
    
    # 备份应用数据
    docker cp kastrax-edutech-app:/app/data "$backup_dir/app_data"
    
    # 备份配置文件
    cp -r "$PRODUCTION_DIR"/{.env,nginx,monitoring} "$backup_dir/"
    
    log_success "备份创建完成: $backup_dir"
}

# 显示部署信息
show_deployment_info() {
    log_success "🎉 Kastrax教育科技AI解决方案部署完成！"
    echo ""
    echo "📋 部署信息:"
    echo "   版本: $VERSION"
    echo "   环境: $ENVIRONMENT"
    echo "   时间: $(date)"
    echo ""
    echo "🌐 访问地址:"
    echo "   应用主页: http://localhost"
    echo "   API文档: http://localhost/swagger-ui.html"
    echo "   健康检查: http://localhost/actuator/health"
    echo "   监控面板: http://localhost:3000 (Grafana)"
    echo "   指标收集: http://localhost:9090 (Prometheus)"
    echo ""
    echo "📊 服务状态:"
    docker-compose -f docker-compose.prod.yml ps
    echo ""
    echo "📝 日志查看:"
    echo "   应用日志: docker-compose -f docker-compose.prod.yml logs -f kastrax-edutech-app"
    echo "   所有日志: docker-compose -f docker-compose.prod.yml logs -f"
    echo ""
    echo "🔧 管理命令:"
    echo "   停止服务: docker-compose -f docker-compose.prod.yml down"
    echo "   重启服务: docker-compose -f docker-compose.prod.yml restart"
    echo "   查看状态: docker-compose -f docker-compose.prod.yml ps"
}

# 主函数
main() {
    log_info "🚀 开始Kastrax教育科技AI解决方案生产环境部署..."
    echo ""
    
    # 检查参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --version)
                VERSION="$2"
                shift 2
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-backup)
                SKIP_BACKUP=true
                shift
                ;;
            --help)
                echo "用法: $0 [选项]"
                echo "选项:"
                echo "  --version VERSION    指定部署版本 (默认: latest)"
                echo "  --skip-tests         跳过测试"
                echo "  --skip-backup        跳过备份"
                echo "  --help              显示帮助信息"
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                exit 1
                ;;
        esac
    done
    
    # 执行部署步骤
    check_prerequisites
    
    if [ "$SKIP_BACKUP" != "true" ]; then
        create_backup
    fi
    
    build_application
    prepare_deployment
    migrate_database
    deploy_services
    health_check
    
    if [ "$SKIP_TESTS" != "true" ]; then
        run_acceptance_tests
    fi
    
    show_deployment_info
    
    log_success "🎉 部署完成！"
}

# 错误处理
trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

# 执行主函数
main "$@"
