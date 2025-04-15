<script setup lang="ts">
// import { ElTooltip } from 'element-plus';
interface NavItem {
  name: string
  path: string
  text: string
  icon: string
}

const navItems: NavItem[] = [
  {
    name: 'home',
    path: '/',
    text: '主页' ,
    icon:'<svg t="1741837654954" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1553" width="32" height="32"><path d="M946.5 505L560.1 118.8l-25.9-25.9c-12.3-12.2-32.1-12.2-44.4 0L77.5 505c-12.3 12.3-18.9 28.6-18.8 46 0.4 35.2 29.7 63.3 64.9 63.3h42.5V940h691.8V614.3h43.4c17.1 0 33.2-6.7 45.3-18.8 12.1-12.1 18.7-28.2 18.7-45.3 0-17-6.7-33.1-18.8-45.2zM568 868H456V664h112v204z m217.9-325.7V868H632V640c0-22.1-17.9-40-40-40H432c-22.1 0-40 17.9-40 40v228H238.1V542.3h-96l370-369.7 23.1 23.1L882 542.3h-96.1z" p-id="1554" fill="currentColor"></path></svg>'
},
  { name: 'diary',
    path: '/diary',
    text: '日记' ,
    icon:'<svg t="1741829007434" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="4513" width="32" height="32"><path d="M725.333333 85.333333H213.333333a42.666667 42.666667 0 0 0-42.666666 42.666667v682.666667a42.666667 42.666667 0 0 0 42.666666 42.666666h42.666667v42.666667a42.666667 42.666667 0 0 0 42.666667 42.666667 42.666667 42.666667 0 0 0 42.666666-42.666667v-42.666667h384a128 128 0 0 0 128-128V213.333333a128 128 0 0 0-128-128z m-128 682.666667H256V170.666667h341.333333z m170.666667-42.666667a42.666667 42.666667 0 0 1-42.666667 42.666667h-42.666666V170.666667h42.666666a42.666667 42.666667 0 0 1 42.666667 42.666666z" p-id="4514" fill="currentColor"></path></svg>'
  },
  {
    name: 'plan',
    path: '/plan',
    text: '规划' ,
    icon:'<svg t="1741829044220" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="6472" width="32" height="32"><path d="M138.688 762.112l186.624 83.008V261.888L138.688 178.88v583.232z m524.736 66.048L400 959.168V960l-0.896-0.384-0.832 0.384-72.96-32.896v-0.32L64 810.688V64l296.64 131.84L624 65.28V64l1.28 0.64 0.832-0.448 41.6 19.2L960 213.376V960l-296.576-131.84zM624 148.608l-224 111.04v616.128l224-111.36V148.608z m74.688 613.504l186.624 83.008V261.888l-186.624-83.008v583.232z" fill="currentColor" p-id="6473"></path></svg>'
  },
  {
    name: 'share',
    path: '/share',
    text: '分享' ,
    icon:'<svg t="1741838493409" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="3093" width="32" height="32"><path d="M736 800c-35.296 0-64-28.704-64-64s28.704-64 64-64 64 28.704 64 64-28.704 64-64 64M288 576c-35.296 0-64-28.704-64-64s28.704-64 64-64 64 28.704 64 64-28.704 64-64 64M736 224c35.296 0 64 28.704 64 64s-28.704 64-64 64-64-28.704-64-64 28.704-64 64-64m0 384a127.776 127.776 0 0 0-115.232 73.28l-204.896-117.056a30.848 30.848 0 0 0-9.696-3.2A127.68 127.68 0 0 0 416 512c0-6.656-0.992-13.088-1.984-19.456 0.608-0.32 1.28-0.416 1.856-0.768l219.616-125.472A127.328 127.328 0 0 0 736 416c70.592 0 128-57.408 128-128s-57.408-128-128-128-128 57.408-128 128c0 6.72 0.992 13.152 1.984 19.616-0.608 0.288-1.28 0.256-1.856 0.608l-219.616 125.472A127.328 127.328 0 0 0 288 384c-70.592 0-128 57.408-128 128s57.408 128 128 128a126.912 126.912 0 0 0 84.544-32.64 31.232 31.232 0 0 0 11.584 12.416l224 128c0.352 0.224 0.736 0.256 1.12 0.448C615.488 812.992 669.6 864 736 864c70.592 0 128-57.408 128-128s-57.408-128-128-128" fill="currentColor" p-id="3094"></path></svg>'
  }
]
</script>

<template>
  <nav class="vertical-nav">
    <el-tooltip
      effect="light"
      placement="right-start"
      :show-arrow="false"
      :enterable="false"
      content="OffGo"
    >
      <router-link to="/" class="nav-logo">
        <img alt="logo" src="@/assets/OffGo_logo.svg" height="48" width="48" />
      </router-link>
    </el-tooltip>
    <router-link
      v-for="item in navItems"
      :key="item.name"
      :to="item.path"
      class="nav-item"
      :class="{ 'is-active': $route.path === item.path }"
    >
      <el-tooltip
        effect="light"
        placement="right-start"
        :show-arrow="false"
        :enterable="false"
        :content="item.text"
      >
        <span class="icon-placeholder" v-html="item.icon"></span>
      </el-tooltip>
<!--      <span class="nav-text">{{ item.text }}</span>-->
    </router-link>

  </nav>
</template>

<style scoped>
.vertical-nav {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 70px;
  padding: 20px 0;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
  border-right: 1px solid 		#DCDCDC;
}

.nav-logo{
  transition: all 0.3s;
  width: 48px;
  height: 48px;
  &:hover{
    transform: scale(1.1);
  }
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 60px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  text-decoration: none;
  color: #606266;
  padding: 10px 0;

  &:hover {
    background-color: #e8e8e8;
    color: #409EFF;
    .icon-placeholder {
      color: #409EFF;
    }
  }

  &.is-active {
    background-color: #e8e8e8;
    color: #409EFF;
    .icon-placeholder {
      color: #409EFF;
    }
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
      background-color: #409EFF;
    }
  }

  .icon-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: inherit;
    transition: color 0.3s;
  }
}

</style>
