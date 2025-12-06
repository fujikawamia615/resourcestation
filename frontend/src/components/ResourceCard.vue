<script setup>
defineProps({
  resource: Object,
  getCoverUrl: Function,
  getResourceDownloadUrl: Function,
  formatSize: Function,
  handleImageError: Function,
});
</script>

<template>
  <div class="resource-card">
    <img
      v-if="resource.cover"
      :src="getCoverUrl(resource.cover)"
      @error="handleImageError"
      class="card-cover"
      alt="封面"
    />
    <div class="card-content">
      <h3 class="card-title">{{ resource.name }}</h3>
      <p class="card-desc">{{ resource.description || '暂无描述' }}</p>
      <div class="card-meta">
        <span class="file-type">{{ resource.fileType || '其他' }}</span>
        <span class="size">{{ formatSize(resource.size) }}</span>
        <span class="download-count">📥 {{ resource.times || 0 }}</span>
      </div>
      <div class="card-actions">
        <a :href="getResourceDownloadUrl(resource)" target="_blank" class="download-btn">📥 下载</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 12px 12px 0 0;
  background: #f5f7ff;
}

.resource-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.3);
  height: fit-content;
  display: flex;
  flex-direction: column;
}

.resource-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(106, 90, 249, 0.2);
}

.card-content {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #222;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 12px;
}

.file-type {
  background: #eef2ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.size {
  color: #666;
  font-size: 12px;
  text-align: center;
}

.download-btn {
  flex: 1;
  background: linear-gradient(90deg, #6a5af9, #8a7bff);
  color: white;
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-align: center;
  display: block;
  border: none;
  cursor: pointer;
}

.download-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(106, 90, 249, 0.3);
}

</style>