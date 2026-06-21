#!/bin/bash

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "❌ Must be sourced: source scripts/docker-deploy.sh"
    exit 1
fi

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
DOCKER_DIR="infrastructure/docker"
AVAILABLE_SERVICES=("api-gateway" "auth-service" "core-service" "configuration-service" "connector-runtime-service" "task-management-service" "identity-service")

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

start-bootstrap() {
    log_info "Starting bootstrap services..."
    docker network create backend-network 2>/dev/null || log_warning "Network exists"
    cd "$DOCKER_DIR" && docker compose -f docker-compose-bootstrap.yml up -d 
    cd - > /dev/null
    log_success "Bootstrap started"
}

stop-bootstrap() {
    log_info "Stopping bootstrap..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose-bootstrap.yml down 
    cd - > /dev/null
    log_success "Bootstrap stopped"
}

build-all() {
    log_info "Building all with Maven..."
    mvn clean install -DskipTests
    log_success "Build completed"
}

docker-build() {
    log_info "Building Docker images..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml build --no-cache 
    cd - > /dev/null
    log_success "Images built"
}

start-services() {
    log_info "Starting all services..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml up -d 
    cd - > /dev/null
    log_success "Services started"
}

start-service() {
    if [[ -z "$1" ]]; then
        log_error "Usage: start-service SERVICE_NAME"
        log_info "Available: ${AVAILABLE_SERVICES[*]}"
        return 1
    fi
    log_info "Starting $1..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml up -d "$1" 
    cd - > /dev/null
    log_success "$1 started"
}

stop-services() {
    log_info "Stopping all services..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml down 
    cd - > /dev/null
    log_success "Services stopped"
}

restart-service() {
    if [[ -z "$1" ]]; then
        log_error "Usage: restart-service SERVICE_NAME"
        return 1
    fi
    log_info "Restarting $1..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml restart "$1" 
    cd - > /dev/null
    log_success "$1 restarted"
}

restart-services() {
    log_info "Restarting all services..."
    cd "$DOCKER_DIR" && docker compose -f docker-compose.yml restart 
    cd - > /dev/null
    log_success "All services restarted"
}

show-status() {
    cd "$DOCKER_DIR"
    echo -e "\n${BLUE}Bootstrap:${NC}"
    docker compose -f docker-compose-bootstrap.yml ps
    echo -e "\n${BLUE}Services:${NC}"
    docker compose -f docker-compose.yml ps
    cd - > /dev/null
}

logs() {
    if [[ -z "$1" ]]; then
        log_error "Usage: logs SERVICE_NAME"
        return 1
    fi
    cd "$DOCKER_DIR"
    if [[ " kafka opensearch kafka-ui " =~ " $1 " ]]; then
        docker compose -f docker-compose-bootstrap.yml logs -f "$1"
    else
        docker compose -f docker-compose.yml logs -f "$1"
    fi
    cd - > /dev/null
}

full-deploy() {
    log_info "Full deployment..."
    docker network create backend-network 2>/dev/null || true
    start-bootstrap
    docker-build
    start-services
    log_success "Deployment completed!"
}

deploy-help() {
    echo -e "${GREEN}COMMANDS:${NC}"
    echo "  start-bootstrap     - Start Kafka, OpenSearch"
    echo "  stop-bootstrap      - Stop bootstrap"
    echo "  build-all          - Maven build"
    echo "  docker-build       - Build images"
    echo "  start-services     - Start all services"
    echo "  start-service NAME - Start one service"
    echo "  stop-services      - Stop all services"
    echo "  restart-service NAME - Restart service"
    echo "  restart-services   - Restart all services"
    echo "  show-status        - Show status"
    echo "  logs NAME          - Follow logs"
    echo "  full-deploy        - Do everything"
    echo ""
    echo -e "${BLUE}Services:${NC} ${AVAILABLE_SERVICES[*]}"
}

log_success "Deployment loaded! Type 'deploy-help'"