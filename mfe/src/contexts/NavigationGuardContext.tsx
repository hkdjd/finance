import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { Modal } from 'antd';

interface NavigationGuard {
  shouldBlock: boolean;
  message: string;
  onConfirm?: () => void;
}

interface NavigationGuardContextType {
  setGuard: (guard: NavigationGuard | null) => void;
  checkNavigation: (onProceed: () => void) => boolean;
}

const NavigationGuardContext = createContext<NavigationGuardContextType | undefined>(undefined);

export const NavigationGuardProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [guard, setGuard] = useState<NavigationGuard | null>(null);

  const checkNavigation = useCallback((onProceed: () => void): boolean => {
    if (!guard || !guard.shouldBlock) {
      onProceed();
      return true;
    }

    Modal.confirm({
      title: '确认操作',
      content: guard.message,
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        guard.onConfirm?.();
        onProceed();
      },
    });

    return false;
  }, [guard]);

  return (
    <NavigationGuardContext.Provider value={{ setGuard, checkNavigation }}>
      {children}
    </NavigationGuardContext.Provider>
  );
};

export const useNavigationGuard = () => {
  const context = useContext(NavigationGuardContext);
  if (!context) {
    throw new Error('useNavigationGuard must be used within NavigationGuardProvider');
  }
  return context;
};
