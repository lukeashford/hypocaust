import React from "react";
import {BrowserRouter, Route, Routes as RouterRoutes} from "react-router-dom";
import ScrollToTop from "components/ScrollToTop";
import ErrorBoundary from "components/ErrorBoundary";
import NotFound from "pages/NotFound";
import ChatInterface from './pages/chat-interface-main-application-screen';
import PDFPreviewAndDownloadScreen from './pages/pdf-preview-and-download-screen';

const Routes = () => {
  return (
      <BrowserRouter>
        <ErrorBoundary>
          <ScrollToTop/>
          <RouterRoutes>
            {/* Define your route here */}
            <Route path="/" element={<ChatInterface/>}/>
            <Route path="/chat-interface-main-application-screen" element={<ChatInterface/>}/>
            <Route path="/pdf-preview-and-download-screen"
                   element={<PDFPreviewAndDownloadScreen/>}/>
            <Route path="*" element={<NotFound/>}/>
          </RouterRoutes>
        </ErrorBoundary>
      </BrowserRouter>
  );
};

export default Routes;
