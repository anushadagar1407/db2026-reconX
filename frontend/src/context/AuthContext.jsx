import React, { createContext, useContext, useState, useEffect } from 'react';

export const AuthContext = createContext({
  user: null,
  isLoading: true,
  login: () => {},
  logout: () => {},
});

function readInitialUser() {
  if (typeof sessionStorage === 'undefined') return null;

  const token = sessionStorage.getItem('reconx-token');
  const role = sessionStorage.getItem('reconx-role');

  return token ? { token, role } : null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initialUser = readInitialUser();
    setUser(initialUser);
    setIsLoading(false);
  }, []);

  const login = (token, role) => {
    sessionStorage.setItem('reconx-token', token);

    if (role) {
      sessionStorage.setItem('reconx-role', role);
    }

    setUser({ token, role });
  };

  const logout = () => {
    sessionStorage.removeItem('reconx-token');
    sessionStorage.removeItem('reconx-role');

    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);