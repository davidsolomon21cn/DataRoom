<template>
  <div class="dataroom-graph-wrapper">
<!--    <el-form-item
      label="颜色"
      class="form-item-box"
    >
      <ColorMultipleSelect
        v-model="config"
        :config="config"
      />
    </el-form-item>-->
    <el-form-item
      label="热点形状"
      class="form-item-box"
    >
      <el-select
        v-model="config.option.shape"
        placeholder="请选择形状"
        @change="changeStyle"
      >
        <el-option
          v-for="item in heatShapeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
<!--    <el-form-item
      label="热点大小"
      class="form-item-box"
    >
      <el-input-number
        v-model="config.option.sizeRatio"
        :min="0"
        :max="1"
        :step="0.1"
        controls-position="right"
        @change="changeStyle"
      />
    </el-form-item>-->
    <el-collapse>
      <el-collapse-item
        title="标签"
      >
        <el-form-item
          label="显示"
          class="form-item-box"
        >
          <el-switch
            v-model="config.option.label.style.opacity"
            :active-value="1"
            :inactive-value="0"
            @change="changeStyle"
          />
        </el-form-item>
        <template v-if="config.option.label.style.opacity">
          <el-form-item
            class="radio form-item-box"
            label="标签位置"
          >
            <el-select
              v-model="config.option.label.position"
              placeholder="请选择"
              @change="changeStyle"
            >
              <el-option
                v-for="item in positionOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            class="radio form-item-box"
            label="相对偏移量"
          >
            <el-row>
              <el-col
                :span="24"
              >
                <el-input-number
                  v-model="config.option.label.offsetX"
                  controls-position="right"
                  @change="changeStyle"
                />
                <div class="set-desc">
                  水平
                </div>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-input-number
                  v-model="config.option.label.offsetY"
                  controls-position="right"
                  @change="changeStyle"
                />
                <div class="set-desc">
                  垂直
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item
            class="radio form-item-box"
            label="文本样式"
          >
            <el-row>
              <el-col
                :span="12"
                style="padding-right: 5px"
              >
                <el-select
                  v-model="config.option.label.style.fontFamily"
                  placeholder="请选择文本样式"
                  @change="changeStyle"
                >
                  <el-option
                    v-for="item in fonFamilyList"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <div class="set-desc">
                  字体
                </div>
              </el-col>
              <el-col
                :span="12"
                style="padding-left: 5px"
              >
                <el-select
                  v-model="config.option.label.style.fontWeight"
                  placeholder="请选择文字粗细"
                  @change="changeStyle"
                >
                  <el-option
                    v-for="item in fontWeightOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <div class="set-desc">
                  文字粗细
                </div>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="18">
                <el-input-number
                  v-model="config.option.label.style.fontSize"
                  controls-position="right"
                  @change="changeStyle"
                />
                <div class="set-desc">
                  字号
                </div>
              </el-col>
              <el-col
                :span="6"
                class="form-item-col"
              >
                <el-color-picker
                  v-model="config.option.label.style.fill"
                  @change="changeStyle"
                />
                <div class="set-desc color-desc">
                  颜色
                </div>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item
            class="radio form-item-box"
            label="文本描边"
          >
            <el-row>
              <el-col :span="18">
                <el-input-number
                  v-model="config.option.label.style.lineWidth"
                  controls-position="right"
                  :min="0"
                  :max="10"
                  @change="changeStyle"
                />
                <div class="set-desc">
                  粗细
                </div>
              </el-col>
              <el-col
                :span="6"
                class="form-item-col"
              >
                <el-color-picker
                  v-model="config.option.label.style.stroke"
                  @change="changeStyle"
                />
                <div
                  class="set-desc color-desc"
                >
                  颜色
                </div>
              </el-col>
            </el-row>
          </el-form-item>
        </template>
      </el-collapse-item>
      <el-collapse-item
        title="热点描边"
      >
        <el-form-item
          label="显示"
          class="form-item-box"
        >
          <el-switch
            v-model="config.option.heatmapStyle.opacity"
            :active-value="1"
            :inactive-value="0"
            @change="changeStyle"
          />
        </el-form-item>
        <template v-if="config.option.heatmapStyle.opacity">
          <el-form-item
            class="radio form-item-box"
            label="粗细"
          >
            <el-input-number
              v-model="config.option.heatmapStyle.lineWidth"
              controls-position="right"
              :min="0"
              @change="changeStyle"
            />
          </el-form-item>
          <el-form-item
            class="radio form-item-box"
            label="颜色"
          >
            <el-color-picker
              v-model="config.option.heatmapStyle.stroke"
              @change="changeStyle"
            />
          </el-form-item>
        </template>
      </el-collapse-item>
      <el-collapse-item
        title="内边距"
      >
        <el-form-item
          label=""
          label-width="0"
          class="form-item-box"
        >
          <el-row>
            <el-col
              :span="12"
              style="padding-right: 5px"
            >
              <el-input-number
                v-model="config.option.appendPadding[0]"
                controls-position="right"
                @change="changeStyle"
              />
              <div class="set-desc">
                上边距
              </div>
            </el-col>
            <el-col
              :span="12"
              style="padding-left: 5px"
            >
              <el-input-number
                v-model="config.option.appendPadding[1]"
                controls-position="right"
                @change="changeStyle"
              />
              <div class="set-desc">
                右边距
              </div>
            </el-col>
          </el-row>
          <el-row>
            <el-col
              :span="12"
              style="padding-right: 5px"
            >
              <el-input-number
                v-model="config.option.appendPadding[2]"
                controls-position="right"
                @change="changeStyle"
              />
              <div class="set-desc">
                下边距
              </div>
            </el-col>
            <el-col
              :span="12"
              style="padding-left: 5px"
            >
              <el-input-number
                v-model="config.option.appendPadding[3]"
                controls-position="right"
                @change="changeStyle"
              />
              <div class="set-desc">
                左边距
              </div>
            </el-col>
          </el-row>
        </el-form-item>
      </el-collapse-item>
      <el-collapse-item
        v-if="currentPageType === 'bigScreen'"
        title="旋转"
      >
        <TransformSet :config="config" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script>
import TransformSet from '@gcpaas/data-room-ui/packages/components/common/panel/TransformSet/index.vue'
import commonMixins from '@gcpaas/data-room-ui/packages/js/mixins/commonMixins'
import GradualSetting from '@gcpaas/data-room-ui/packages/components/common/panel/GradualSetting/index.vue'
import {
  fontWeightOptions,
  fonFamilyList,
  positionOptions,
  heatShapeOptions
} from '@gcpaas/data-room-ui/packages/js/utils/options'
import ColorMultipleSelect from '@gcpaas/data-room-ui/packages/components/common/panel/ColorMultipleSelect/index.vue'
export default {
  name: '',
  components: {
    ColorMultipleSelect,
    GradualSetting,
    TransformSet
  },
  mixins: [commonMixins],
  inject: ['canvasInst'],
  props: {
    config: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      fontWeightOptions,
      fonFamilyList,
      positionOptions,
      heatShapeOptions
    }
  },
  computed: {
    currentPageType () {
      return this.canvasInst.currentPageType
    }
  },
  watch: {},
  created () {},
  mounted () {},
  methods: {}
}
</script>

<style scoped lang="scss">
@import '@gcpaas/data-room-ui/packages/assets/style/settingWrap.scss';
</style>
