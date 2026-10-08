import React, { createContext, useContext, useState, useEffect } from 'react';
import { getCustomerProfile } from '../services/customer/customerService';

interface CustomerContextType {
  isCustomer: boolean | null;
  loading: boolean;
  refreshCustomerStatus: () => Promise<void>;
}

const CustomerContext = createContext<CustomerContextType>({
  isCustomer: null,
  loading: true,
  refreshCustomerStatus: async () => {},
});

export const CustomerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isCustomer, setIsCustomer] = useState<boolean | null>(null);
  const [loading, setLoading] = useState(true);

  const checkCustomerStatus = async () => {
    try {
      setLoading(true);
      // Try to fetch customer profile - if successful, user is a customer
      await getCustomerProfile();
      setIsCustomer(true);
    } catch (error: any) {
      // If we get 400/404/Customer not found, user is not a customer yet
      if (error.message?.includes('400') || error.message?.includes('404') || error.message?.includes('Customer not found')) {
        setIsCustomer(false);
      } else {
        // Other errors - assume not a customer to be safe
        console.error('Error checking customer status:', error);
        setIsCustomer(false);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkCustomerStatus();
  }, []);

  return (
    <CustomerContext.Provider value={{ isCustomer, loading, refreshCustomerStatus: checkCustomerStatus }}>
      {children}
    </CustomerContext.Provider>
  );
};

export const useCustomerContext = () => useContext(CustomerContext);
