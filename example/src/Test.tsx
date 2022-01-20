import { IonPage, IonButton, useIonLoading, useIonToast } from '@ionic/react';
import { Http } from '@alitajs/http';
import { Directory, Filesystem } from '@capacitor/filesystem';
import { useState } from 'react';

const TestPage = () => {
  const [present, dismiss] = useIonLoading();
  const [showToast] = useIonToast();
  const [destPath, setDestPath] = useState('');
  const download = async () => {
    try {
      present('下载中...');
      const url = 'https://www2.deloitte.com/content/dam/Deloitte/cn/Documents/consumer-business/deloitte-cn-cb-brand-customer-experience-in-now-consumer-zh-210507.pdf';
      const fileName = url.split('/').pop()?.split('#')[0].split('?')[0];
      const filePath = `Downloads/${fileName}`;
      // const status = await Filesystem.stat({ path: filePath, directory: Directory.Documents });
      try {
        await Filesystem.mkdir({ path: 'Downloads', directory: Directory.Documents });
      } catch (error) {}
      try {
        await Filesystem.deleteFile({ path: filePath, directory: Directory.Documents });
      } catch (error) {}
      const result = await Http.downloadFile({
        url,
        filePath: `Downloads/${fileName}`,
        fileDirectory: Directory.Documents,
      });
      console.log('result', result.path);
      setDestPath(result.path || '');
      dismiss();
    } catch (error: any) {
      console.log(error.message);
      dismiss();
      showToast(error.message, 2000);
    }
  };
  return (
    <IonPage className={'page'}>
      <div>
        <IonButton fill="solid" expand="full" shape="round" onClick={download}>
          DownloadFile
        </IonButton>
        <div>
          <code>{destPath}</code>
        </div>
      </div>
    </IonPage>
  );
};

export default TestPage;
