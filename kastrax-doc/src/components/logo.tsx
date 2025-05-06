export const logo = (
  <svg
    width={24}
    height={23}
    viewBox="0 0 114 34"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className="w-[5.5rem] md:w-[7.5rem] md:h-[2rem]"
  >
    {/* 超级自行车图标 */}
    <g transform="translate(0, 5) scale(0.8)">
      {/* 后轮 */}
      <circle cx="15" cy="20" r="8" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 前轮 */}
      <circle cx="35" cy="20" r="8" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 车架 */}
      <path d="M15 20 L25 10 L35 20" stroke="currentColor" strokeWidth="1.5" fill="none" />
      <path d="M25 10 L22 20" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 车把 */}
      <path d="M25 10 L28 7" stroke="currentColor" strokeWidth="1.5" fill="none" />
      <path d="M28 7 L31 10" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 座椅 */}
      <path d="M22 10 L22 12" stroke="currentColor" strokeWidth="1.5" fill="none" />
      <path d="M20 10 L24 10" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 踏板 */}
      <circle cx="22" cy="20" r="2" stroke="currentColor" strokeWidth="1.5" fill="none" />
      <path d="M22 18 L24 15" stroke="currentColor" strokeWidth="1.5" fill="none" />

      {/* 超级元素：火焰 */}
      <path d="M7 20 C5 18 6 15 7 14 C8 16 10 15 9 18 C11 17 12 20 10 22 C9 21 7 22 7 20 Z" fill="currentColor" />
      <path d="M43 20 C45 18 44 15 43 14 C42 16 40 15 41 18 C39 17 38 20 40 22 C41 21 43 22 43 20 Z" fill="currentColor" />
    </g>

    {/* 文字 */}
    <text x="50" y="22" fill="currentColor" fontFamily="Arial" fontSize="16" fontWeight="bold">kastrax</text>
  </svg>
);

export const LogoWithSuffix = () => (
  <div className="flex items-center gap-2">{logo}</div>
);
