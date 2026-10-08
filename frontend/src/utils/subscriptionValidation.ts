/**
 * Validation rules based on backend DTOs
 */
export const validateCustomerDetails = (name: string, value: string) => {
  switch (name) {
    case 'phone':
      if (!value) return 'Phone number is required';
      if (!/^[0-9]{10,15}$/.test(value)) return 'Phone number must be 10-15 digits';
      break;
    case 'state':
      if (!value) return 'State is required';
      if (value.length > 100) return 'State must be less than 100 characters';
      break;
    case 'city':
      if (!value) return 'City is required';
      if (value.length > 100) return 'City must be less than 100 characters';
      break;
    case 'addressLine1':
      if (!value) return 'Address is required';
      if (value.length > 255) return 'Address must be less than 255 characters';
      break;
    case 'postalCode':
      if (!value) return 'Postal code is required';
      if (value.length > 20) return 'Postal code must be less than 20 characters';
      break;
  }
  return '';
};

export const validatePaymentMethod = (name: string, value: string, paymentType: 'CARD' | 'UPI') => {
  if (paymentType === 'CARD') {
    switch (name) {
      case 'cardNumber':
        if (!/^[0-9]{16}$/.test(value)) return 'Card number must be 16 digits';
        break;
      case 'cardholderName':
        if (!value) return 'Cardholder name is required';
        if (value.length > 100) return 'Cardholder name must be less than 100 characters';
        if (!/^[a-zA-Z\s]+$/.test(value)) return 'Cardholder name must contain only letters and spaces';
        break;
      case 'expiryMonth':
        if (!/^(0[1-9]|1[0-2])$/.test(value)) return 'Expiry month must be 01-12';
        break;
      case 'expiryYear':
        if (!/^[0-9]{4}$/.test(value)) return 'Expiry year must be 4 digits';
        break;
      case 'cvv':
        if (!/^[0-9]{3,4}$/.test(value)) return 'CVV must be 3-4 digits';
        break;
    }
  } else {
    if (name === 'upiId') {
      if (!/^[a-zA-Z0-9._-]+@[a-zA-Z]+$/.test(value)) return 'Invalid UPI ID format';
    }
  }
  return '';
};
export const validateExpiryDate = (month: string, year: string) => {
  if (!month || !year) return '';
  if (!/^(0[1-9]|1[0-2])$/.test(month) || !/^[0-9]{4}$/.test(year)) return '';

  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1; // 0-indexed

  const expYear = parseInt(year);
  const expMonth = parseInt(month);

  if (expYear < currentYear) {
    return 'Expiry year must be in the future';
  }
  if (expYear === currentYear && expMonth < currentMonth) {
    return 'Expiry month cannot be in the past';
  }
  return '';
};
