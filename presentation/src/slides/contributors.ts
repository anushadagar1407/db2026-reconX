export interface Contributor {
  displayName: string;
  login: string;
  avatarSrc: string;
}

const contributorAsset = (fileName: string) => (
  `${import.meta.env.BASE_URL}assets/contributors/${fileName}`
);

export const contributors: readonly Contributor[] = [
  {
    displayName: 'Tobias Becher',
    login: 'TB-DevAcc',
    avatarSrc: contributorAsset('tb-devacc.jpg'),
  },
  {
    displayName: 'gb-dev04',
    login: 'gb-dev04',
    avatarSrc: contributorAsset('gb-dev04.jpg'),
  },
  {
    displayName: 'NJ Bodam',
    login: 'NJBodam',
    avatarSrc: contributorAsset('njbodam.jpg'),
  },
  {
    displayName: 'Timur Garipov',
    login: 'SaltyRain',
    avatarSrc: contributorAsset('saltyrain.jpg'),
  },
  {
    displayName: 'anushadagar1407',
    login: 'anushadagar1407',
    avatarSrc: contributorAsset('anushadagar1407.jpg'),
  },
  {
    displayName: 'Aqib Khan',
    login: 'Aqibkhan2023',
    avatarSrc: contributorAsset('aqibkhan2023.jpg'),
  },
  {
    displayName: 'Beyda',
    login: 'beydarb',
    avatarSrc: contributorAsset('beydarb.jpg'),
  },
];
