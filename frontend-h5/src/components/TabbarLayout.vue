<template>
  <div class="page-with-tabbar">
    <slot />
    <van-tabbar v-model="active" route active-color="#10a37f" inactive-color="#9ca3af" class="main-tabbar" safe-area-inset-bottom>
      <van-tabbar-item to="/square" icon="home-o">广场</van-tabbar-item>
      <van-tabbar-item to="/post/create" class="publish-item">
        <template #icon>
          <div class="publish-btn">
            <van-icon name="plus" />
          </div>
        </template>
        发布
      </van-tabbar-item>
      <van-tabbar-item to="/mine" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const active = ref(0)
</script>

<style scoped lang="scss">
.main-tabbar {
  // 悬浮胶囊：离底 12px，全 pill 圆角，毛玻璃
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: 12px;
  width: auto;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 20px 48px -24px rgba(0, 0, 0, 0.18);

  .publish-item {
    :deep(.van-tabbar-item__icon) {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    // 选中（进入发布页）时 + 号旋转 45° 成 ×，动能反馈
    &.van-tabbar-item--active {
      .publish-btn {
        transform: rotate(45deg);
        background: var(--primary-dark);
      }
    }
  }

  .publish-btn {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--primary-gradient);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 16px -6px rgba(16, 163, 127, 0.5);
    transition: transform 0.4s var(--ease), background 0.3s var(--ease),
      box-shadow 0.3s var(--ease);

    .van-icon {
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      transition: transform 0.4s var(--ease);
    }

    &:active {
      transform: scale(0.92);
    }
  }
}
</style>
