import React, { lazy, Suspense } from 'react';
import { Spin } from 'antd';

// ResourceForm接口定义
export interface ResourceFormProps {
  resourceType: string;
  driverMode?: 'SOURCE' | 'TARGET';
  formData?: Record<string, any>;
  onChange?: (values: Record<string, any>) => void;
  onSubmit?: (values: Record<string, any>) => void;
  loading?: boolean;
  readOnly?: boolean;
}

// 资源表单组件映射
const resourceFormComponents: Record<string, any> = {
  // 消息队列和协议
  'MQTT': lazy(() => import('./MqttForm')),
  'KAFKA': lazy(() => import('./KafkaForm')),
  
  // 规则组件
  'RULE_ENGINE': lazy(() => import('./RuleEngineProperties')),
  'CONDITION_RULE': lazy(() => import('./ConditionRuleProperties')),
  'THRESHOLD_RULE': lazy(() => import('./ThresholdRuleProperties')),
  'TIMING_RULE': lazy(() => import('./TimingRuleProperties')),
  'EXPRESSION_RULE': lazy(() => import('./ExpressionRuleProperties')),
};

// 资源表单工厂函数
export function createResourceForm(resourceType: string, props: Omit<ResourceFormProps, 'resourceType'>) {
  const FormComponent = resourceFormComponents[resourceType];
  
  if (FormComponent) {
    return (
      <Suspense fallback={<Spin />}>
        <FormComponent {...props} />
      </Suspense>
    );
  }
  
  // 默认占位符
  return (
    <div>
      <h3>资源表单: {resourceType}</h3>
      {props.loading ? <Spin /> : <div>资源类型: {resourceType}</div>}
      {props.driverMode && <div>驱动模式: {props.driverMode}</div>}
    </div>
  );
}

// ResourceForm 组件
const ResourceForm: React.FC<ResourceFormProps> = (props) => {
  const { resourceType, ...restProps } = props;
  return createResourceForm(resourceType, restProps);
};

export default ResourceForm; 