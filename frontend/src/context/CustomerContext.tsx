import React, { createContext, useContext, useState, useEffect } from 'react';
import { getCustomerProfile } from '../services/customer/customerService';
import { errorMessage } from '../utils/apiError';

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
    } catch (error) {
      // If we get 400/404/Customer not found, user is not a customer yet
      if (errorMessage(error).includes('400') || errorMessage(error).includes('404') || errorMessage(error).includes('Customer not found')) {
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
    // eslint-disable-next-line react-hooks/set-state-in-effect -- loads data on mount; state only changes once the request settles
    checkCustomerStatus();
  }, []);

  return (
    <CustomerContext.Provider value={{ isCustomer, loading, refreshCustomerStatus: checkCustomerStatus }}>
      {children}
    </CustomerContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components -- context modules export their provider and hook together
export const useCustomerContext = () => useContext(CustomerContext);
