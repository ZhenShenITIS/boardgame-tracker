import { createTheme } from '@mantine/core';

export const appTheme = createTheme({
  primaryColor: 'ocean',
  colors: {
    ocean: ['#e7edf6', '#d5e0ef', '#b2c6e0', '#8aaacd', '#6490be', '#4e82b6', '#3f78b3', '#31689f', '#285b8f', '#194c81'],
    ember: ['#fff1e7', '#ffe1cf', '#ffc39f', '#ff9d66', '#ff7d3b', '#ff6820', '#ff5c10', '#e44b04', '#cb4100', '#b13600'],
  },
  defaultRadius: 'md',
  headings: {
    fontFamily: 'Segoe UI, Inter, sans-serif',
  },
  fontFamily: 'Segoe UI, Inter, sans-serif',
});
