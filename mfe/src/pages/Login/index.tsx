import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message, Space } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined } from '@ant-design/icons';
import styles from './styles.module.css';

const { Title, Text } = Typography;

interface LoginFormData {
  username: string;
  password: string;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  // 检查是否已登录
  useEffect(() => {
    const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
    if (isLoggedIn) {
      navigate('/page-a');
    }
  }, [navigate]);

  // 禁用页面滚动
  useEffect(() => {
    // 保存原始overflow值
    const originalOverflow = document.body.style.overflow;
    // 禁用滚动
    document.body.style.overflow = 'hidden';

    // 组件卸载时恢复
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, []);

  // 处理登录提交
  const handleSubmit = async (values: LoginFormData) => {
    setLoading(true);

    try {
      // 模拟网络延迟
      await new Promise(resolve => setTimeout(resolve, 1000));

      const { username, password } = values;

      // 表单验证
      if (!username.trim()) {
        message.error('请输入用户名');
        form.setFields([
          {
            name: 'username',
            errors: ['用户名不能为空'],
          },
        ]);
        return;
      }

      if (!password) {
        message.error('请输入密码');
        form.setFields([
          {
            name: 'password',
            errors: ['密码不能为空'],
          },
        ]);
        return;
      }

      // 密码验证（固定密码 123456）
      if (password !== '123456') {
        message.error('密码错误，请重新输入');
        form.setFields([
          {
            name: 'password',
            errors: ['密码错误'],
          },
        ]);
        return;
      }

      // 登录成功
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('username', username);
      message.success(`欢迎回来，${username}！`);

      // 跳转到首页
      navigate('/page-a');

    } catch (error) {
      message.error('登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 处理表单验证失败
  const handleFormFailed = (errorInfo: any) => {
    console.log('表单验证失败:', errorInfo);
  };

  return (
    <div className={styles.loginContainer}>
      {/* Logo */}
      <div className={styles.logo}>
        <img
          src="/OCBC.png"
          alt="OCBC Logo"
          className={styles.logoImage}
        />
      </div>

      <Card className={styles.loginCard}>
        <div className={styles.loginHeader}>
          <Title level={2} style={{ color: '#b5120f', marginBottom: 8 }}>
            会计功能管理系统
          </Title>
          <Text type="secondary" style={{ fontSize: 16 }}>
            请登录您的账户
          </Text>
        </div>

        <Form
          form={form}
          name="login"
          className={styles.loginForm}
          onFinish={handleSubmit}
          onFinishFailed={handleFormFailed}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, message: '用户名至少2个字符' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入用户名"
              className={styles.loginInput}
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
              className={styles.loginInput}
            />
          </Form.Item>

          <Form.Item>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                className={styles.loginButton}
                icon={<LoginOutlined />}
                block
              >
                {loading ? '登录中...' : '登录'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {/* 版权信息 */}
      <div className={styles.copyright}>
        <Text style={{ fontSize: 12, color: '#4A4A4A' }}>
          ©华侨银行中国版权所有 . 保留所有权 . 本网站支持IPv6
        </Text>
      </div>
    </div>
  );
};

export default Login;
