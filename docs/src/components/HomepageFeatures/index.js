import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Easy to Use',
    description: (
      <>
        Write your code. Package it up. Upload. Deploy. Sip coffee.
      </>
    ),
  },
  {
    title: 'Flexible',
    description: (
      <>
        Use any library you want. Call any service you want. Save database data. Send text messages. Send emails.
      </>
    ),
  },
  {
    title: 'Minimal Dev-Ops',
    description: (
      <>
        Function orchestration is built-in.
      </>
    ),
  },
];

function Feature({ title, description }) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
