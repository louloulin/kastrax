/**
 * API 路径映射配置
 * 
 * 这个配置文件定义了 Dataflare-UI 和 Datalink-UI 之间的 API 路径映射关系，
 * 便于集中管理和更新。
 */

export const apiMapping = {
  auth: {
    login: {
      v1: '/api/v1/auth/login',
      v2: '/api/v2/auth/login',
      legacy: '/api/auth/user/login'
    },
    logout: {
      v1: '/api/v1/auth/logout',
      v2: '/api/v2/auth/logout',
      legacy: '/auth/logout'
    },
    info: {
      v1: '/api/v1/auth/info',
      v2: '/api/v2/auth/info',
      legacy: '/api/auth/user/info'
    },
    nav: {
      v1: '/api/v1/auth/nav',
      v2: '/api/v2/auth/nav',
      legacy: '/api/auth/user/nav'
    }
  },
  rule: {
    list: {
      v1: '/api/v1/rules',
      v2: '/api/v2/rules',
      legacy: '/api/rule/list'
    },
    info: {
      v1: '/api/v1/rules/:id',
      v2: '/api/v2/rules/:id',
      legacy: '/api/rule/info'
    },
    add: {
      v1: '/api/v1/rules',
      v2: '/api/v2/rules',
      legacy: '/api/rule/add'
    },
    update: {
      v1: '/api/v1/rules/:id',
      v2: '/api/v2/rules/:id',
      legacy: '/api/rule/update'
    },
    remove: {
      v1: '/api/v1/rules/:id',
      v2: '/api/v2/rules/:id',
      legacy: '/api/rule/remove'
    },
    start: {
      v1: '/api/v1/rules/:id/start',
      v2: '/api/v2/rules/:id/start',
      legacy: '/api/rule/start'
    },
    stop: {
      v1: '/api/v1/rules/:id/stop',
      v2: '/api/v2/rules/:id/stop',
      legacy: '/api/rule/stop'
    },
    startAndSave: {
      v1: '/api/v1/rules/:id/start-and-save',
      v2: '/api/v2/rules/:id/start-and-save',
      legacy: '/api/rule/startAndSave'
    },
    restart: {
      v1: '/api/v1/rules/:id/restart',
      v2: '/api/v2/rules/:id/restart',
      legacy: '/api/rule/restart'
    },
    runtime: {
      v1: '/api/v1/runtime/rule/:id',
      v2: '/api/v2/runtime/rule/:id',
      legacy: '/api/runtime/rule/info'
    },
    execute: {
      v1: '/api/v1/rules/:id/execute',
      v2: '/api/v2/rules/:id/execute',
      legacy: '/api/rule/execute'
    }
  },
  resource: {
    list: {
      v1: '/api/v1/resources',
      v2: '/api/v2/resources',
      legacy: '/api/resource/list'
    },
    info: {
      v1: '/api/v1/resources/:id',
      v2: '/api/v2/resources/:id',
      legacy: '/api/resource/info'
    },
    add: {
      v1: '/api/v1/resources',
      v2: '/api/v2/resources',
      legacy: '/api/resource/add'
    },
    update: {
      v1: '/api/v1/resources/:id',
      v2: '/api/v2/resources/:id',
      legacy: '/api/resource/update'
    },
    remove: {
      v1: '/api/v1/resources/:id',
      v2: '/api/v2/resources/:id',
      legacy: '/api/resource/remove'
    },
    test: {
      v1: '/api/v1/resources/test',
      v2: '/api/v2/resources/test',
      legacy: '/api/resource/test'
    },
    types: {
      v1: '/api/v1/resources/types',
      v2: '/api/v2/resources/types',
      legacy: '/api/resource/types'
    }
  },
  script: {
    list: {
      v1: '/api/v1/scripts',
      v2: '/api/v2/scripts',
      legacy: '/api/script/list'
    },
    info: {
      v1: '/api/v1/scripts/:id',
      v2: '/api/v2/scripts/:id',
      legacy: '/api/script/info'
    },
    add: {
      v1: '/api/v1/scripts',
      v2: '/api/v2/scripts',
      legacy: '/api/script/add'
    },
    update: {
      v1: '/api/v1/scripts/:id',
      v2: '/api/v2/scripts/:id',
      legacy: '/api/script/update'
    },
    remove: {
      v1: '/api/v1/scripts/:id',
      v2: '/api/v2/scripts/:id',
      legacy: '/api/script/remove'
    },
    test: {
      v1: '/api/v1/scripts/:id/test',
      v2: '/api/v2/scripts/:id/test',
      legacy: '/api/script/test'
    },
    execute: {
      v1: '/api/v1/scripts/:id/execute',
      v2: '/api/v2/scripts/:id/execute',
      legacy: '/api/script/execute'
    },
    debug: {
      v1: '/api/v1/scripts/:id/debug',
      v2: '/api/v2/scripts/:id/debug',
      legacy: '/api/script/test' // 降级为测试
    },
    dependencies: {
      v1: '/api/v1/scripts/:id/dependencies',
      v2: '/api/v2/scripts/:id/dependencies',
      legacy: '/api/script/dependencies'
    },
    installDependencies: {
      v1: '/api/v1/scripts/:id/dependencies/install',
      v2: '/api/v2/scripts/:id/dependencies/install',
      legacy: '/api/script/dependencies/install'
    }
  },
  system: {
    info: {
      v1: '/api/v1/system/info',
      v2: '/api/v2/system/info',
      legacy: '/api/system/info'
    },
    statistics: {
      v1: '/api/v1/system/statistics',
      v2: '/api/v2/system/statistics',
      legacy: '/api/system/statistics'
    }
  }
};
