import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import ScriptsPage from './page';
import * as scriptsApi from '@/lib/api/scripts';

// ... existing code ...

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <BrowserRouter>
      <Toaster />
      {ui}
    </BrowserRouter>
  );
};

// ... existing code ...